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
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

@Service
class IngredientService(
    private val ingredientRepository: IngredientRepository,
    private val storageTipRepository: StorageTipRepository,
    private val storageCautionRepository: StorageCautionRepository,
    private val ingredientPairingRepository: IngredientPairingRepository,
    private val ingredientAiService: IngredientAiService,
    private val ingredientAiSaver: IngredientAiSaver,
) {

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
     * AI 재료 생성 요청. 이미 존재하면 즉시 반환, 없으면 AI 생성을 비동기로 트리거.
     * - COMPLETED → 200
     * - PENDING   → 202 (클라이언트 폴링 유도)
     * - FAILED    → PENDING 리셋 후 재시도 → 202
     */
    fun generate(keyword: String): IngredientStatusResponse {
        require(keyword.isNotBlank()) { "검색어를 입력해주세요." }

        // 1. 원본 keyword로 빠른 조회 (alias로 등록된 경우 hit)
        ingredientRepository.findByKeyword(keyword)?.let {
            return resolveStatus(it)
        }

        // 2. AI 유효성 검증 + 정규화 (DB write 없음)
        //    "브로컬리" → "브로콜리" 처럼 오타·비표준 표기를 표준명으로 교정
        val normalized = ingredientAiService.validateAndNormalize(keyword)

        // 3. 정규명으로 재조회 (다른 표기가 이미 저장된 경우 hit)
        if (normalized != keyword) {
            ingredientRepository.findByKeyword(normalized)?.let {
                return resolveStatus(it)
            }
        }

        // 4. PENDING 선점 저장 시도 (name unique 제약으로 동시 중복 삽입 차단)
        return try {
            val pending = ingredientAiSaver.savePending(normalized)
            ingredientAiService.generateAndSave(pending.id, normalized)
            IngredientStatusResponse.pending(pending.id)
        } catch (e: DataIntegrityViolationException) {
            // 동시 요청으로 다른 스레드가 먼저 삽입한 경우 → 기존 row 반환
            val existing = ingredientRepository.findByKeyword(normalized)
                ?: throw IllegalStateException("중복 저장 감지 후 재조회 실패: keyword=$normalized")
            resolveStatus(existing)
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
        if (ingredient.status != IngredientStatus.COMPLETED) {
            throw IllegalArgumentException("아직 처리 중인 재료입니다. status=${ingredient.status}")
        }
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