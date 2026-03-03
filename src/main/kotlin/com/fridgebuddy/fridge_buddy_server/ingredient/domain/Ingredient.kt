package com.fridgebuddy.fridge_buddy_server.ingredient.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "ingredients")
class Ingredient(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val category: CategoryType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val storageType: StorageType,

    @Column(columnDefinition = "TEXT")
    val storageTip: String? = null,

    @Column(columnDefinition = "TEXT")
    val storageDescription: String? = null,

    @Column(nullable = false)
    val defaultExpirationDays: Int,
)