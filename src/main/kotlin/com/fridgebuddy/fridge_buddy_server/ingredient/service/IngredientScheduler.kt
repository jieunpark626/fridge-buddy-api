package com.fridgebuddy.fridge_buddy_server.ingredient.service

import com.fridgebuddy.fridge_buddy_server.ingredient.domain.IngredientStatus
import com.fridgebuddy.fridge_buddy_server.ingredient.repository.IngredientRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class IngredientScheduler(
    private val ingredientRepository: IngredientRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 30초 이상 PENDING 상태인 재료를 FAILED로 전환.
     * 서버 재시작 등으로 인한 stuck PENDING 방지.
     */
    @Scheduled(fixedDelay = 10_000)
    @Transactional
    fun expireStuckPending() {
        val threshold = LocalDateTime.now().minusSeconds(120)
        val count = ingredientRepository.expirePendingOlderThan(
            threshold = threshold,
            pending = IngredientStatus.PENDING,
            failed = IngredientStatus.FAILED,
        )
        if (count > 0) {
            log.warn("stuck PENDING 재료 {}건을 FAILED로 전환했습니다.", count)
        }
    }
}