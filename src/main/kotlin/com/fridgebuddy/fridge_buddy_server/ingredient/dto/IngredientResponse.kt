package com.fridgebuddy.fridge_buddy_server.ingredient.dto

import com.fridgebuddy.fridge_buddy_server.ingredient.domain.CategoryType
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.Ingredient
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.StorageType

data class IngredientResponse(
    val id: Long,
    val name: String,
    val icon: String?,
    val category: CategoryType,
    val defaultStorageType: StorageType,
    val shelfLifeText: String,
) {
    companion object {
        fun from(ingredient: Ingredient) = IngredientResponse(
            id = ingredient.id,
            name = ingredient.name,
            icon = ingredient.icon,
            category = ingredient.category,
            defaultStorageType = ingredient.defaultStorageType,
            shelfLifeText = ingredient.shelfLifeText,
        )
    }
}
