package com.fridgebuddy.fridge_buddy_server.ingredient.service

import com.fridgebuddy.fridge_buddy_server.ingredient.domain.*
import com.fridgebuddy.fridge_buddy_server.ingredient.dto.IngredientAiDto
import com.fridgebuddy.fridge_buddy_server.ingredient.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * AI 흐름에서 발생하는 모든 DB write를 담당.
 * IngredientAiService(@Async)에서 self-invocation 없이 호출하기 위해 별도 빈으로 분리.
 */
@Service
class IngredientAiSaver(
    private val ingredientRepository: IngredientRepository,
    private val storageTipRepository: StorageTipRepository,
    private val storageCautionRepository: StorageCautionRepository,
    private val ingredientAliasRepository: IngredientAliasRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** keyword로 PENDING 재료를 선점 저장하고 즉시 커밋 */
    @Transactional
    fun savePending(keyword: String): Ingredient {
        val ingredient = ingredientRepository.save(
            Ingredient(
                name = keyword,
                category = CategoryType.VEGETABLE,  // AI가 덮어씀
                defaultStorageType = StorageType.FRIDGE,  // AI가 덮어씀
                shelfLifeText = "",
                storageMethod = "",
                status = IngredientStatus.PENDING,
                pendingSince = LocalDateTime.now(),
            )
        )
        log.info("[AiSaver] PENDING 저장: keyword={}, id={}", keyword, ingredient.id)
        return ingredient
    }

    /** AI 생성 성공 시 전체 데이터를 하나의 트랜잭션으로 저장 */
    @Transactional
    fun save(ingredientId: Long, aiData: IngredientAiDto, keyword: String) {
        val ingredient = ingredientRepository.findById(ingredientId)
            .orElseThrow { IllegalStateException("PENDING 재료를 찾을 수 없습니다. id=$ingredientId") }

        log.info("[AiSaver] save 시작: id={}, 기존 name={}, AI 정규명={}", ingredientId, ingredient.name, aiData.name)

        // 정규명 및 모든 필드 업데이트
        ingredient.name = aiData.name
        ingredient.icon = aiData.icon
        ingredient.category = aiData.category
        ingredient.defaultStorageType = aiData.defaultStorageType
        ingredient.shelfLifeText = aiData.shelfLifeText
        ingredient.shelfLifeDaysFridge = aiData.shelfLifeDaysFridge
        ingredient.shelfLifeDaysFreezer = aiData.shelfLifeDaysFreezer
        ingredient.shelfLifeDaysRoomTemp = aiData.shelfLifeDaysRoomTemp
        ingredient.storageMethod = aiData.storageMethod
        ingredient.extraInfo = aiData.extraInfo
        ingredient.status = IngredientStatus.COMPLETED
        ingredient.pendingSince = null

        storageTipRepository.saveAll(
            aiData.tips.map { StorageTip(ingredient = ingredient, stepOrder = it.stepOrder, content = it.content) }
        )
        storageCautionRepository.saveAll(
            aiData.cautions.map { StorageCaution(ingredient = ingredient, content = it.content, cautionType = it.cautionType) }
        )

        // 정규명과 다른 표현만 alias로 저장 (keyword + AI가 제안한 동의어)
        // 이미 다른 재료의 alias로 등록된 항목은 skip (대소문자 무관)
        val aliases = (aiData.aliases + keyword)
            .map { it.trim() }
            .filter { it.isNotBlank() && it != aiData.name }
            .filter { !ingredientAliasRepository.existsByAliasIgnoreCase(it) }
            .distinct()
        log.info("[AiSaver] alias 저장: id={}, aliases={}", ingredientId, aliases)
        ingredientAliasRepository.saveAll(
            aliases.map { IngredientAlias(ingredient = ingredient, alias = it) }
        )

        log.info("[AiSaver] COMPLETED 저장 완료: id={}, name={}", ingredientId, aiData.name)
    }

    /** AI 호출 실패 시 FAILED 업데이트 (save 트랜잭션과 별도로 커밋) */
    @Transactional
    fun markFailed(ingredientId: Long) {
        log.warn("[AiSaver] FAILED 마킹: id={}", ingredientId)
        ingredientRepository.updateStatus(ingredientId, IngredientStatus.FAILED, null)
    }

    /** FAILED 재료를 PENDING으로 리셋하여 재시도 허용 */
    @Transactional
    fun resetToPending(ingredientId: Long) {
        log.info("[AiSaver] PENDING 리셋: id={}", ingredientId)
        ingredientRepository.updateStatus(ingredientId, IngredientStatus.PENDING, LocalDateTime.now())
    }
}