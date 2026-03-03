package com.fridgebuddy.fridge_buddy_server.ingredient.controller

import com.fridgebuddy.fridge_buddy_server.common.response.ApiResponse
import com.fridgebuddy.fridge_buddy_server.ingredient.dto.IngredientDetailResponse
import com.fridgebuddy.fridge_buddy_server.ingredient.dto.IngredientResponse
import com.fridgebuddy.fridge_buddy_server.ingredient.service.IngredientService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/ingredients")
class IngredientController(
    private val ingredientService: IngredientService,
) {

    @GetMapping
    fun search(@RequestParam name: String): ApiResponse<List<IngredientResponse>> =
        ApiResponse.ok(ingredientService.search(name))

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ApiResponse<IngredientDetailResponse> =
        ApiResponse.ok(ingredientService.getById(id))
}
