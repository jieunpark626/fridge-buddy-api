package com.fridgebuddy.fridge_buddy_server.auth.toss

import com.fridgebuddy.fridge_buddy_server.auth.JwtProvider
import com.fridgebuddy.fridge_buddy_server.fridge.repository.FridgeItemRepository
import com.fridgebuddy.fridge_buddy_server.user.domain.SocialProvider
import com.fridgebuddy.fridge_buddy_server.user.domain.User
import com.fridgebuddy.fridge_buddy_server.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TossLoginService(
    private val tossPartnerApiClient: TossPartnerApiClient,
    private val userRepository: UserRepository,
    private val fridgeItemRepository: FridgeItemRepository,
    private val jwtProvider: JwtProvider,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * authorizationCode + referrer를 받아 토스 API 교환 → 내부 JWT 발급
     */
    @Transactional
    fun login(authorizationCode: String, referrer: String): String {
        val tokenResponse = tossPartnerApiClient.generateToken(authorizationCode, referrer)
        val userInfo = tossPartnerApiClient.getLoginMe(tokenResponse.accessToken)

        val user = upsertUser(userInfo.userKey)
        return jwtProvider.createToken(user.id)
    }

    /**
     * 우리 서비스에서 로그아웃 — Toss 측 AccessToken도 무효화
     * 유저가 존재하지 않으면 no-op (이미 탈퇴한 경우 등)
     */
    fun logout(userId: Long) {
        val user = userRepository.findById(userId).orElse(null) ?: return
        val userKey = user.providerId.toLongOrNull() ?: return
        runCatching { tossPartnerApiClient.removeByUserKey(userKey) }
            .onFailure { log.warn("Toss 토큰 무효화 실패: userKey=$userKey", it) }
    }

    @Transactional
    fun unlinkUser(userKey: Long) {
        val user = userRepository.findByProviderAndProviderId(SocialProvider.TOSS, userKey.toString())
            ?: return
        fridgeItemRepository.deleteByUserId(user.id)
        userRepository.delete(user)
    }

    private fun upsertUser(userKey: Long): User {
        val providerId = userKey.toString()
        return userRepository.findByProviderAndProviderId(SocialProvider.TOSS, providerId)
            ?: userRepository.save(
                User(
                    provider = SocialProvider.TOSS,
                    providerId = providerId,
                )
            )
    }
}