package com.fridgebuddy.fridge_buddy_server.ingredient.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "ingredients")
class Ingredient(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto increment
    val id: Long = 0,

    // AI가 생성 후 정규명으로 덮어씀 → var
    @Column(nullable = false, unique = true, length = 50)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: CategoryType,

    // 재료를 대표하는 이모지 (AI가 생성)
    @Column(length = 10)
    var icon: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var defaultStorageType: StorageType,

    @Column(nullable = false, length = 50)
    var shelfLifeText: String,

    // AUTO 유통기한 계산용 (보관 방법별 일수, AI 생성 시 함께 저장)
    @Column
    var shelfLifeDaysFridge: Int? = null,

    @Column
    var shelfLifeDaysFreezer: Int? = null,

    @Column
    var shelfLifeDaysRoomTemp: Int? = null,

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