package com.fridgebuddy.fridge_buddy_server.ingredient.service

import com.fridgebuddy.fridge_buddy_server.ingredient.dto.IngredientDetailResponse
import com.fridgebuddy.fridge_buddy_server.ingredient.dto.IngredientResponse
import com.fridgebuddy.fridge_buddy_server.ingredient.repository.IngredientPairingRepository
import com.fridgebuddy.fridge_buddy_server.ingredient.repository.IngredientRepository
import com.fridgebuddy.fridge_buddy_server.ingredient.repository.StorageCautionRepository
import com.fridgebuddy.fridge_buddy_server.ingredient.repository.StorageTipRepository
import org.springframework.stereotype.Service

@Service
class IngredientService(
    private val ingredientRepository: IngredientRepository,
    private val storageTipRepository: StorageTipRepository,
    private val storageCautionRepository: StorageCautionRepository,
    private val ingredientPairingRepository: IngredientPairingRepository,
) {

    fun search(name: String): List<IngredientResponse> {
        require(name.isNotBlank()) { "검색어를 입력해주세요." }
        return ingredientRepository.findByNameContainingIgnoreCase(name)
            .map { IngredientResponse.from(it) }
    }

    fun getById(id: Long): IngredientDetailResponse {
        val ingredient = ingredientRepository.findById(id)
            .orElseThrow { NoSuchElementException("식재료를 찾을 수 없습니다. id=$id") }
        val tips = storageTipRepository.findByIngredientIdOrderByStepOrder(id)
        val cautions = storageCautionRepository.findByIngredientId(id)
        val pairings = ingredientPairingRepository.findByIngredientId(id)
        return IngredientDetailResponse.of(ingredient, tips, cautions, pairings)
    }
}
