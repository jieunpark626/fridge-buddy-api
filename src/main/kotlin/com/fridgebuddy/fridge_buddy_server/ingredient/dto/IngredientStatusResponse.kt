package com.fridgebuddy.fridge_buddy_server.ingredient.dto

import com.fridgebuddy.fridge_buddy_server.ingredient.domain.IngredientStatus

data class IngredientStatusResponse(
    val id: Long,
    val status: IngredientStatus,
    val data: IngredientDetailResponse? = null,
    val message: String? = null,
) {
    companion object {
        fun pending(id: Long) = IngredientStatusResponse(
            id = id,
            status = IngredientStatus.PENDING,
        )

        fun completed(data: IngredientDetailResponse) = IngredientStatusResponse(
            id = data.id,
            status = IngredientStatus.COMPLETED,
            data = data,
        )

        fun failed(id: Long) = IngredientStatusResponse(
            id = id,
            status = IngredientStatus.FAILED,
            message = "재료 정보 생성에 실패했습니다. 잠시 후 다시 시도해주세요.",
        )
    }
}