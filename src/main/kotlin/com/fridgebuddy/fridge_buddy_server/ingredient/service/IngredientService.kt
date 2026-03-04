package com.fridgebuddy.fridge_buddy_server.ingredient.service

import com.fridgebuddy.fridge_buddy_server.ingredient.domain.Ingredient
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.IngredientStatus
import com.fridgebuddy.fridge_buddy_server.ingredient.dto.IngredientDetailResponse
import com.fridgebuddy.fridge_buddy_server.ingredient.dto.IngredientResponse
import com.fridgebuddy.fridge_buddy_server.ingredient.dto.IngredientStatusResponse
import com.fridgebuddy.fridge_buddy_server.ingredient.repository.IngredientPairingRepository
import com.fridgebuddy.fridge_buddy_server.ingredient.repository.IngredientRepository
import com.fridgebuddy.fridge_buddy_server.ingredient.repository.StorageCautionRepository
import com.fridgebuddy.fridge_buddy_server.ingredient.repository.StorageTipRepository
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

@Service
class IngredientService(
    private val ingredientRepository: IngredientRepository,
    private val storageTipRepository: StorageTipRepository,
    private val storageCautionRepository: StorageCautionRepository,
    private val ingredientPairingRepository: IngredientPairingRepository,
    private val ingredientAiService: IngredientAiService,
    private val ingredientAiSaver: IngredientAiSaver,
) {
    // keyword 단위 JVM 락: 동일 키워드 동시 요청 시 AI 중복 호출 방지
    private val keywordLocks = ConcurrentHashMap<String, ReentrantLock>()

    /** 전체 조회 또는 부분 일치 검색 (alias 포함, COMPLETED 재료만 반환) */
    fun search(name: String?): List<IngredientResponse> {
        if (name.isNullOrBlank()) {
            return ingredientRepository.findByStatus(IngredientStatus.COMPLETED)
                .map { IngredientResponse.from(it) }
        }
        return ingredientRepository.findByNameOrAliasContainingIgnoreCaseAndStatus(name, IngredientStatus.COMPLETED)
            .map { IngredientResponse.from(it) }
    }

    /**
     * 정확한 키워드로 조회. DB에 없으면 AI 생성을 비동기로 트리거.
     * - COMPLETED → 200
     * - PENDING   → 202 (클라이언트 폴링 유도)
     * - FAILED    → PENDING 리셋 후 재시도 → 202
     */
    fun lookup(keyword: String): IngredientStatusResponse {
        require(keyword.isNotBlank()) { "검색어를 입력해주세요." }

        // 1. alias OR name 조회
        ingredientRepository.findByKeyword(keyword)?.let { return resolveStatus(it) }

        // 2. 없음 → keyword 단위 락 획득
        val lock = keywordLocks.computeIfAbsent(keyword) { ReentrantLock() }
        lock.lock()
        try {
            // 3. double-check (락 대기 중 다른 스레드가 이미 저장했을 수 있음)
            ingredientRepository.findByKeyword(keyword)?.let { return resolveStatus(it) }

            // 4. 식재료 유효성 검증 (동기, InvalidIngredientException 시 422 반환)
            ingredientAiService.validate(keyword)

            // 5. PENDING 선점 저장 (별도 트랜잭션으로 즉시 커밋)
            val pending = ingredientAiSaver.savePending(keyword)

            // 6. AI 비동기 호출 (PENDING 커밋 이후)
            ingredientAiService.generateAndSave(pending.id, keyword)

            return IngredientStatusResponse.pending(pending.id)
        } finally {
            lock.unlock()
        }
    }

    /** 폴링용: ID로 현재 상태 조회 */
    fun getStatusById(id: Long): IngredientStatusResponse {
        val ingredient = ingredientRepository.findById(id)
            .orElseThrow { NoSuchElementException("식재료를 찾을 수 없습니다. id=$id") }
        return resolveStatus(ingredient)
    }

    /** ID로 완성된 재료 상세 조회 (기존 엔드포인트용) */
    fun getById(id: Long): IngredientDetailResponse {
        val ingredient = ingredientRepository.findById(id)
            .orElseThrow { NoSuchElementException("식재료를 찾을 수 없습니다. id=$id") }
        check(ingredient.status == IngredientStatus.COMPLETED) { "아직 처리 중인 재료입니다." }
        return buildDetail(ingredient)
    }

    private fun resolveStatus(ingredient: Ingredient): IngredientStatusResponse = when (ingredient.status) {
        IngredientStatus.COMPLETED -> IngredientStatusResponse.completed(buildDetail(ingredient))
        IngredientStatus.PENDING -> IngredientStatusResponse.pending(ingredient.id)
        IngredientStatus.FAILED -> {
            // 재시도: PENDING으로 리셋 후 AI 재호출
            ingredientAiSaver.resetToPending(ingredient.id)
            ingredientAiService.generateAndSave(ingredient.id, ingredient.name)
            IngredientStatusResponse.pending(ingredient.id)
        }
    }

    private fun buildDetail(ingredient: Ingredient): IngredientDetailResponse {
        val tips = storageTipRepository.findByIngredientIdOrderByStepOrder(ingredient.id)
        val cautions = storageCautionRepository.findByIngredientId(ingredient.id)
        val pairings = ingredientPairingRepository.findByIngredientId(ingredient.id)
        return IngredientDetailResponse.of(ingredient, tips, cautions, pairings)
    }
}