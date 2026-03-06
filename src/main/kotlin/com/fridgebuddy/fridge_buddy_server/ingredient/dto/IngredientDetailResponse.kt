package com.fridgebuddy.fridge_buddy_server.ingredient.dto

import com.fridgebuddy.fridge_buddy_server.ingredient.domain.CategoryType
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.Ingredient
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.IngredientPairing
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.StorageCaution
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.StorageTip
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.StorageType

data class IngredientDetailResponse(
    val id: Long,
    val name: String,
    val icon: String?,
    val category: CategoryType,
    val defaultStorageType: StorageType,
    val shelfLifeText: String,
    val storageMethod: String,
    val extraInfo: String?,
    val tips: List<StorageTipResponse>,
    val cautions: List<StorageCautionResponse>,
    val pairings: List<PairingResponse>,
) {
    companion object {
        fun of(
            ingredient: Ingredient,
            tips: List<StorageTip>,
            cautions: List<StorageCaution>,
            pairings: List<IngredientPairing>,
        ) = IngredientDetailResponse(
            id = ingredient.id,
            name = ingredient.name,
            icon = ingredient.icon,
            category = ingredient.category,
            defaultStorageType = ingredient.defaultStorageType,
            shelfLifeText = ingredient.shelfLifeText,
            storageMethod = ingredient.storageMethod,
            extraInfo = ingredient.extraInfo,
            tips = tips.map { StorageTipResponse.from(it) },
            cautions = cautions.map { StorageCautionResponse.from(it) },
            pairings = pairings.map { PairingResponse.of(ingredient.id, it) },
        )
    }
}

data class StorageTipResponse(
    val stepOrder: Int,
    val content: String,
) {
    companion object {
        fun from(tip: StorageTip) = StorageTipResponse(
            stepOrder = tip.stepOrder,
            content = tip.content,
        )
    }
}

data class StorageCautionResponse(
    val content: String,
    val cautionType: String,
) {
    companion object {
        fun from(caution: StorageCaution) = StorageCautionResponse(
            content = caution.content,
            cautionType = caution.cautionType,
        )
    }
}

data class PairingResponse(
    val ingredientId: Long,
    val ingredientName: String,
    val pairingType: String,
) {
    companion object {
        fun of(currentId: Long, pairing: IngredientPairing): PairingResponse {
            val other = if (pairing.ingredientA.id == currentId) pairing.ingredientB else pairing.ingredientA
            return PairingResponse(
                ingredientId = other.id,
                ingredientName = other.name,
                pairingType = pairing.pairingType,
            )
        }
    }
}
