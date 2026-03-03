package com.fridgebuddy.fridge_buddy_server.ingredient.dto

import com.fridgebuddy.fridge_buddy_server.ingredient.domain.CategoryType
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.Ingredient
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.StorageType

data class IngredientResponse(
    val id: Long,
    val name: String,
    val category: CategoryType,
    val storageType: StorageType,
    val storageTip: String?,
    val storageDescription: String?,
    val defaultExpirationDays: Int,
) {
    companion object {
        fun from(ingredient: Ingredient) = IngredientResponse(
            id = ingredient.id,
            name = ingredient.name,
            category = ingredient.category,
            storageType = ingredient.storageType,
            storageTip = ingredient.storageTip,
            storageDescription = ingredient.storageDescription,
            defaultExpirationDays = ingredient.defaultExpirationDays,
        )
    }
}