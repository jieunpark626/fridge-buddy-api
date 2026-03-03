package com.fridgebuddy.fridge_buddy_server.ingredient.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "ingredients")
class Ingredient(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 50)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val category: CategoryType,

    @Column(length = 255)
    val imageUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val defaultStorageType: StorageType,

    @Column(nullable = false, length = 50)
    val shelfLifeText: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val storageMethod: String,

    @Column(columnDefinition = "TEXT")
    val extraInfo: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
