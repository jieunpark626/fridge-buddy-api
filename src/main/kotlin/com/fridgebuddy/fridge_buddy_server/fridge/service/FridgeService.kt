package com.fridgebuddy.fridge_buddy_server.fridge.service

import com.fridgebuddy.fridge_buddy_server.common.exception.DuplicateFridgeItemException
import com.fridgebuddy.fridge_buddy_server.fridge.domain.ExpiryInputType
import com.fridgebuddy.fridge_buddy_server.fridge.domain.FridgeItem
import com.fridgebuddy.fridge_buddy_server.fridge.dto.AddFridgeItemRequest
import com.fridgebuddy.fridge_buddy_server.fridge.dto.FridgeItemDetailResponse
import com.fridgebuddy.fridge_buddy_server.fridge.dto.FridgeItemSummaryResponse
import com.fridgebuddy.fridge_buddy_server.fridge.dto.UpdateQuantityRequest
import com.fridgebuddy.fridge_buddy_server.fridge.repository.FridgeItemRepository
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.Ingredient
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.StorageType
import com.fridgebuddy.fridge_buddy_server.ingredient.repository.IngredientRepository
import com.fridgebuddy.fridge_buddy_server.ingredient.repository.StorageCautionRepository
import com.fridgebuddy.fridge_buddy_server.ingredient.repository.StorageTipRepository
import com.fridgebuddy.fridge_buddy_server.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class FridgeService(
    private val fridgeItemRepository: FridgeItemRepository,
    private val ingredientRepository: IngredientRepository,
    private val storageTipRepository: StorageTipRepository,
    private val storageCautionRepository: StorageCautionRepository,
    private val userRepository: UserRepository,
) {

    @Transactional
    fun addItem(userId: Long, request: AddFridgeItemRequest): FridgeItemDetailResponse {
        val ingredient = ingredientRepository.findById(request.ingredientId)
            .orElseThrow { NoSuchElementException("식재료를 찾을 수 없습니다. id=${request.ingredientId}") }

        if (fridgeItemRepository.existsByUserIdAndIngredientId(userId, request.ingredientId)) {
            throw DuplicateFridgeItemException("이미 냉장고에 등록된 재료입니다.")
        }

        require(request.quantity > java.math.BigDecimal.ZERO) { "수량은 0보다 커야 합니다." }

        val expiryDate = calculateExpiryDate(request, ingredient)
        val user = userRepository.getReferenceById(userId)

        val saved = fridgeItemRepository.save(
            FridgeItem(
                user = user,
                ingredient = ingredient,
                storageType = request.storageType,
                quantity = request.quantity,
                quantityUnit = request.quantityUnit,
                purchasedAt = request.purchasedAt,
                expiryDate = expiryDate,
                expiryInputType = request.expiryInputType,
                memo = request.memo,
            )
        )
        return buildDetail(saved)
    }

    fun getItems(userId: Long): List<FridgeItemSummaryResponse> =
        fridgeItemRepository.findByUserIdOrderByExpiryDateAsc(userId)
            .map { FridgeItemSummaryResponse.from(it) }

    fun getItem(userId: Long, id: Long): FridgeItemDetailResponse =
        buildDetail(findItem(userId, id))

    @Transactional
    fun updateQuantity(userId: Long, id: Long, request: UpdateQuantityRequest): FridgeItemSummaryResponse {
        require(request.quantity >= java.math.BigDecimal.ZERO) { "수량은 0 이상이어야 합니다." }
        val item = findItem(userId, id)
        item.quantity = request.quantity
        item.updatedAt = LocalDateTime.now()
        return FridgeItemSummaryResponse.from(item)
    }

    @Transactional
    fun deleteItem(userId: Long, id: Long) = fridgeItemRepository.delete(findItem(userId, id))

    private fun findItem(userId: Long, id: Long): FridgeItem =
        fridgeItemRepository.findByIdAndUserId(id, userId)
            ?: throw NoSuchElementException("냉장고 재료를 찾을 수 없습니다. id=$id")

    private fun calculateExpiryDate(request: AddFridgeItemRequest, ingredient: Ingredient): LocalDate =
        when (request.expiryInputType) {
            ExpiryInputType.MANUAL -> {
                val date = requireNotNull(request.expiryDate) { "유통기한 날짜를 입력해주세요." }
                require(!date.isBefore(LocalDate.now())) { "유통기한은 오늘 이후 날짜여야 합니다." }
                date
            }
            ExpiryInputType.AUTO -> {
                val purchasedAt = requireNotNull(request.purchasedAt) { "구매일을 입력해주세요." }
                val days = getShelfLifeDays(ingredient, request.storageType)
                    ?: throw IllegalArgumentException("해당 보관 방법의 유통기한 정보가 없습니다. 직접 입력해주세요.")
                purchasedAt.plusDays(days.toLong())
            }
        }

    private fun getShelfLifeDays(ingredient: Ingredient, storageType: StorageType): Int? =
        when (storageType) {
            StorageType.FRIDGE -> ingredient.shelfLifeDaysFridge
            StorageType.FREEZER -> ingredient.shelfLifeDaysFreezer
            StorageType.ROOM_TEMP -> ingredient.shelfLifeDaysRoomTemp
        }

    private fun buildDetail(item: FridgeItem): FridgeItemDetailResponse {
        val tips = storageTipRepository.findByIngredientIdOrderByStepOrder(item.ingredient.id)
        val cautions = storageCautionRepository.findByIngredientId(item.ingredient.id)
        return FridgeItemDetailResponse.of(item, tips, cautions)
    }
}
