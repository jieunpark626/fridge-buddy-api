package com.fridgebuddy.fridge_buddy_server.ingredient.repository

import com.fridgebuddy.fridge_buddy_server.ingredient.domain.StorageTip
import org.springframework.data.jpa.repository.JpaRepository

interface StorageTipRepository : JpaRepository<StorageTip, Long> {

    fun findByIngredientIdOrderByStepOrder(ingredientId: Long): List<StorageTip>
}
