package com.fridgebuddy.fridge_buddy_server.fridge.repository

import com.fridgebuddy.fridge_buddy_server.fridge.domain.FridgeItem
import org.springframework.data.jpa.repository.JpaRepository

interface FridgeItemRepository : JpaRepository<FridgeItem, Long> {

    fun findByUserIdOrderByExpiryDateAsc(userId: Long): List<FridgeItem>

    fun existsByUserIdAndIngredientId(userId: Long, ingredientId: Long): Boolean
}