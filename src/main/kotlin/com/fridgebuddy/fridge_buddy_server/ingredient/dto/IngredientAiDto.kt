package com.fridgebuddy.fridge_buddy_server.ingredient.dto

import com.fridgebuddy.fridge_buddy_server.ingredient.domain.CategoryType
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.StorageType

data class IngredientAiDto(
    val name: String,
    val category: CategoryType,
    val defaultStorageType: StorageType,
    val shelfLifeText: String,
    val storageMethod: String,
    val extraInfo: String?,
    val aliases: List<String>,
    val tips: List<TipDto>,
    val cautions: List<CautionDto>,
) {
    data class TipDto(val stepOrder: Int, val content: String)
    data class CautionDto(val content: String, val cautionType: String)
}