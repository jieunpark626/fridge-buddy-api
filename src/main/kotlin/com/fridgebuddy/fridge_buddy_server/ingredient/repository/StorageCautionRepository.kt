package com.fridgebuddy.fridge_buddy_server.ingredient.repository

import com.fridgebuddy.fridge_buddy_server.ingredient.domain.StorageCaution
import org.springframework.data.jpa.repository.JpaRepository

interface StorageCautionRepository : JpaRepository<StorageCaution, Long> {

    fun findByIngredientId(ingredientId: Long): List<StorageCaution>
}
