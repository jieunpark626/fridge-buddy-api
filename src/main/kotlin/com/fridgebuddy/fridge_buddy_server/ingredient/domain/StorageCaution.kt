package com.fridgebuddy.fridge_buddy_server.ingredient.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "storage_caution")
class StorageCaution(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    val ingredient: Ingredient,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(length = 20)
    val cautionType: String = "WARNING",

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
