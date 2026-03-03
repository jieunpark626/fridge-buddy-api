package com.fridgebuddy.fridge_buddy_server.ingredient.service

import com.fridgebuddy.fridge_buddy_server.ingredient.dto.IngredientResponse
import com.fridgebuddy.fridge_buddy_server.ingredient.repository.IngredientRepository
import org.springframework.stereotype.Service

@Service
class IngredientService(
    private val ingredientRepository: IngredientRepository,
) {

    fun search(name: String): List<IngredientResponse> {
        require(name.isNotBlank()) { "검색어를 입력해주세요." }
        return ingredientRepository.findByNameContainingIgnoreCase(name)
            .map { IngredientResponse.from(it) }
    }

    fun getById(id: Long): IngredientResponse {
        val ingredient = ingredientRepository.findById(id)
            .orElseThrow { NoSuchElementException("식재료를 찾을 수 없습니다. id=$id") }
        return IngredientResponse.from(ingredient)
    }
}