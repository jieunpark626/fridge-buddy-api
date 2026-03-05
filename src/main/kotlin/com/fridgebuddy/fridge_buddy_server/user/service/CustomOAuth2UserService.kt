package com.fridgebuddy.fridge_buddy_server.user.service

import com.fridgebuddy.fridge_buddy_server.user.domain.SocialProvider
import com.fridgebuddy.fridge_buddy_server.user.domain.User
import com.fridgebuddy.fridge_buddy_server.user.dto.NaverOAuth2UserInfo
import com.fridgebuddy.fridge_buddy_server.user.dto.OAuth2UserInfo
import com.fridgebuddy.fridge_buddy_server.user.repository.UserRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomOAuth2UserService(
    private val userRepository: UserRepository,
) : org.springframework.security.oauth2.client.userinfo.OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private val delegate = DefaultOAuth2UserService()

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = delegate.loadUser(userRequest)
        val registrationId = userRequest.clientRegistration.registrationId

        val (provider, userInfo) = when (registrationId.lowercase()) {
            "naver" -> SocialProvider.NAVER to NaverOAuth2UserInfo(oAuth2User.attributes)
            else -> throw IllegalArgumentException("지원하지 않는 소셜 로그인: $registrationId")
        }

        val user = upsertUser(provider, userInfo)

        // userId를 principal로 사용하기 위해 attributes에 userId 추가
        val attributes = oAuth2User.attributes.toMutableMap()
        attributes["userId"] = user.id

        val nameAttributeKey = userRequest.clientRegistration.providerDetails
            .userInfoEndpoint.userNameAttributeName

        return DefaultOAuth2User(
            oAuth2User.authorities,
            attributes,
            nameAttributeKey,
        )
    }

    private fun upsertUser(provider: SocialProvider, userInfo: OAuth2UserInfo): User {
        val existing = userRepository.findByProviderAndProviderId(provider, userInfo.id)
        return if (existing != null) {
            existing.nickname = userInfo.nickname
            existing.profileImage = userInfo.profileImage
            existing
        } else {
            userRepository.save(
                User(
                    provider = provider,
                    providerId = userInfo.id,
                    nickname = userInfo.nickname,
                    profileImage = userInfo.profileImage,
                )
            )
        }
    }
}
