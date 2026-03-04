package com.fridgebuddy.fridge_buddy_server.fridge.dto

import com.fridgebuddy.fridge_buddy_server.fridge.domain.ExpiryInputType
import com.fridgebuddy.fridge_buddy_server.fridge.domain.QuantityUnit
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.StorageType
import java.math.BigDecimal
import java.time.LocalDate

data class AddFridgeItemRequest(
    val ingredientId: Long,
    val storageType: StorageType,
    val quantity: BigDecimal,
    val quantityUnit: QuantityUnit,
    val purchasedAt: LocalDate? = null,     // AUTO 시 필수
    val expiryInputType: ExpiryInputType,
    val expiryDate: LocalDate? = null,      // MANUAL 시 필수
    val memo: String? = null,
)