package com.fridgebuddy.fridge_buddy_server.ingredient.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "ingredients")
class Ingredient(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // AI가 생성 후 정규명으로 덮어씀 → var
    @Column(nullable = false, unique = true, length = 50)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: CategoryType,

    @Column(length = 255)
    val imageUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var defaultStorageType: StorageType,

    @Column(nullable = false, length = 50)
    var shelfLifeText: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var storageMethod: String,

    @Column(columnDefinition = "TEXT")
    var extraInfo: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: IngredientStatus = IngredientStatus.COMPLETED,

    // PENDING 시작 시각 (stuck 감지용)
    @Column
    var pendingSince: LocalDateTime? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)