package com.fridgebuddy.fridge_buddy_server.ingredient.repository

import com.fridgebuddy.fridge_buddy_server.ingredient.domain.IngredientPairing
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface IngredientPairingRepository : JpaRepository<IngredientPairing, Long> {

    @Query("SELECT p FROM IngredientPairing p WHERE p.ingredientA.id = :ingredientId OR p.ingredientB.id = :ingredientId")
    fun findByIngredientId(ingredientId: Long): List<IngredientPairing>
}