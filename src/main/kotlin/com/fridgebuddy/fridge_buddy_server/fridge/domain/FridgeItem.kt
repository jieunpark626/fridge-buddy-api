package com.fridgebuddy.fridge_buddy_server.fridge.domain

import com.fridgebuddy.fridge_buddy_server.ingredient.domain.Ingredient
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.StorageType
import com.fridgebuddy.fridge_buddy_server.user.domain.User
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "fridge_items",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "ingredient_id"])],
)
class FridgeItem(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    val ingredient: Ingredient,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val storageType: StorageType,

    @Column(nullable = false, precision = 10, scale = 1)
    var quantity: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val quantityUnit: QuantityUnit,

    @Column
    val purchasedAt: LocalDate? = null,

    @Column(nullable = false)
    val expiryDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val expiryInputType: ExpiryInputType,

    @Column(columnDefinition = "TEXT")
    val memo: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)