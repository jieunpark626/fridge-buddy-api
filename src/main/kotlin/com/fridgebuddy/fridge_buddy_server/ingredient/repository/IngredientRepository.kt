package com.fridgebuddy.fridge_buddy_server.ingredient.repository

import com.fridgebuddy.fridge_buddy_server.ingredient.domain.Ingredient
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.IngredientStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface IngredientRepository : JpaRepository<Ingredient, Long> {

    fun findByStatus(status: IngredientStatus): List<Ingredient>

    @Query("""
        SELECT DISTINCT i FROM Ingredient i
        LEFT JOIN IngredientAlias a ON a.ingredient = i
        WHERE (upper(i.name) LIKE upper(concat('%', :name, '%'))
            OR upper(a.alias) LIKE upper(concat('%', :name, '%')))
          AND i.status = :status
    """)
    fun findByNameOrAliasContainingIgnoreCaseAndStatus(
        @Param("name") name: String,
        @Param("status") status: IngredientStatus,
    ): List<Ingredient>

    // alias OR name 동시 조회 (단건)
    @Query("""
        SELECT DISTINCT i FROM Ingredient i
        LEFT JOIN IngredientAlias a ON a.ingredient = i
        WHERE i.name = :keyword OR a.alias = :keyword
    """)
    fun findByKeyword(@Param("keyword") keyword: String): Ingredient?

    // status + pendingSince 업데이트
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Ingredient i SET i.status = :status, i.pendingSince = :pendingSince WHERE i.id = :id")
    fun updateStatus(
        @Param("id") id: Long,
        @Param("status") status: IngredientStatus,
        @Param("pendingSince") pendingSince: LocalDateTime?,
    )

    // 30초 이상 PENDING 상태인 재료를 FAILED로 전환 (스케줄러용)
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Ingredient i
        SET i.status = :failed, i.pendingSince = null
        WHERE i.status = :pending AND i.pendingSince < :threshold
    """)
    fun expirePendingOlderThan(
        @Param("threshold") threshold: LocalDateTime,
        @Param("pending") pending: IngredientStatus = IngredientStatus.PENDING,
        @Param("failed") failed: IngredientStatus = IngredientStatus.FAILED,
    ): Int
}