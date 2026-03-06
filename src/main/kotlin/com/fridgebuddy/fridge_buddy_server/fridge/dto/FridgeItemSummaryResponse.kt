package com.fridgebuddy.fridge_buddy_server.fridge.dto

import com.fridgebuddy.fridge_buddy_server.fridge.domain.FridgeItem
import com.fridgebuddy.fridge_buddy_server.fridge.domain.QuantityUnit
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.CategoryType
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.StorageType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class FridgeItemSummaryResponse(
    val id: Long,
    val ingredientId: Long,
    val ingredientName: String,
    val icon: String?,
    val category: CategoryType,
    val storageType: StorageType,
    val quantity: BigDecimal,
    val quantityUnit: QuantityUnit,
    val expiryDate: LocalDate,
    val daysLeft: Long,
    val isExpiringSoon: Boolean,
    val isExpired: Boolean,
) {
    companion object {
        fun from(item: FridgeItem): FridgeItemSummaryResponse {
            val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), item.expiryDate)
            return FridgeItemSummaryResponse(
                id = item.id,
                ingredientId = item.ingredient.id,
                ingredientName = item.ingredient.name,
                icon = item.ingredient.icon,
                category = item.ingredient.category,
                storageType = item.storageType,
                quantity = item.quantity,
                quantityUnit = item.quantityUnit,
                expiryDate = item.expiryDate,
                daysLeft = daysLeft,
                isExpiringSoon = daysLeft in 0..3,
                isExpired = daysLeft < 0,
            )
        }
    }
}