package com.fridgebuddy.fridge_buddy_server.fridge.dto

import com.fridgebuddy.fridge_buddy_server.fridge.domain.ExpiryInputType
import com.fridgebuddy.fridge_buddy_server.fridge.domain.FridgeItem
import com.fridgebuddy.fridge_buddy_server.fridge.domain.QuantityUnit
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.CategoryType
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.StorageCaution
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.StorageTip
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.StorageType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class FridgeItemDetailResponse(
    val id: Long,
    val ingredientId: Long,
    val ingredientName: String,
    val icon: String?,
    val category: CategoryType,
    val storageType: StorageType,
    val quantity: BigDecimal,
    val quantityUnit: QuantityUnit,
    val purchasedAt: LocalDate?,
    val expiryDate: LocalDate,
    val expiryInputType: ExpiryInputType,
    val daysLeft: Long,
    val isExpiringSoon: Boolean,
    val isExpired: Boolean,
    val memo: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val ingredientDetail: IngredientInfo,
) {
    data class IngredientInfo(
        val storageMethod: String,
        val extraInfo: String?,
        val tips: List<TipInfo>,
        val cautions: List<CautionInfo>,
    )

    data class TipInfo(val stepOrder: Int, val content: String)
    data class CautionInfo(val content: String, val cautionType: String)

    companion object {
        fun of(
            item: FridgeItem,
            tips: List<StorageTip>,
            cautions: List<StorageCaution>,
        ): FridgeItemDetailResponse {
            val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), item.expiryDate)
            return FridgeItemDetailResponse(
                id = item.id,
                ingredientId = item.ingredient.id,
                ingredientName = item.ingredient.name,
                icon = item.ingredient.icon,
                category = item.ingredient.category,
                storageType = item.storageType,
                quantity = item.quantity,
                quantityUnit = item.quantityUnit,
                purchasedAt = item.purchasedAt,
                expiryDate = item.expiryDate,
                expiryInputType = item.expiryInputType,
                daysLeft = daysLeft,
                isExpiringSoon = daysLeft in 0..3,
                isExpired = daysLeft < 0,
                memo = item.memo,
                createdAt = item.createdAt,
                updatedAt = item.updatedAt,
                ingredientDetail = IngredientInfo(
                    storageMethod = item.ingredient.storageMethod,
                    extraInfo = item.ingredient.extraInfo,
                    tips = tips.map { TipInfo(it.stepOrder, it.content) },
                    cautions = cautions.map { CautionInfo(it.content, it.cautionType) },
                ),
            )
        }
    }
}