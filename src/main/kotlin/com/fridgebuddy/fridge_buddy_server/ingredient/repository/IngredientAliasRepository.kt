package com.fridgebuddy.fridge_buddy_server.ingredient.repository

import com.fridgebuddy.fridge_buddy_server.ingredient.domain.IngredientAlias
import org.springframework.data.jpa.repository.JpaRepository

interface IngredientAliasRepository : JpaRepository<IngredientAlias, Long>