package com.fridgebuddy.fridge_buddy_server.ingredient.dto

import com.fridgebuddy.fridge_buddy_server.ingredient.domain.CategoryType
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.Ingredient
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.StorageType

data class IngredientResponse(
    val id: Long,
    val name: String,
    val category: CategoryType,
    val imageUrl: String?,
    val defaultStorageType: StorageType,
    val shelfLifeText: String,
) {
    companion object {
        fun from(ingredient: Ingredient) = IngredientResponse(
            id = ingredient.id,
            name = ingredient.name,
            category = ingredient.category,
            imageUrl = ingredient.imageUrl,
            defaultStorageType = ingredient.defaultStorageType,
            shelfLifeText = ingredient.shelfLifeText,
        )
    }
}
