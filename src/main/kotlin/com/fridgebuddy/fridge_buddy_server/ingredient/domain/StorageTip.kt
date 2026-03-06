package com.fridgebuddy.fridge_buddy_server.ingredient.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "storage_tips")
class StorageTip(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    val ingredient: Ingredient,

    @Column(nullable = false)
    val stepOrder: Int,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
