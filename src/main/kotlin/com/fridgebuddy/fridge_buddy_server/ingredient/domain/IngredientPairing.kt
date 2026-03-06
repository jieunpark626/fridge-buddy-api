package com.fridgebuddy.fridge_buddy_server.ingredient.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "ingredient_pairing",
    uniqueConstraints = [UniqueConstraint(columnNames = ["ingredient_a_id", "ingredient_b_id"])],
)
class IngredientPairing(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // 항상 작은 ID → ingredient_a, 큰 ID → ingredient_b (Check: ingredient_a_id < ingredient_b_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_a_id", nullable = false)
    val ingredientA: Ingredient,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_b_id", nullable = false)
    val ingredientB: Ingredient,

    @Column(length = 20, nullable = false)
    val pairingType: String,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
