package com.fridgebuddy.fridge_buddy_server.auth

import com.fridgebuddy.fridge_buddy_server.auth.dto.LoginResponse
import com.fridgebuddy.fridge_buddy_server.common.response.ApiResponse
import com.fridgebuddy.fridge_buddy_server.user.domain.SocialProvider
import com.fridgebuddy.fridge_buddy_server.user.domain.User
import com.fridgebuddy.fridge_buddy_server.user.repository.UserRepository
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 로컬 개발용 로그인 — dev 프로파일에서만 활성화됩니다.
 * 토스앱 밖에서는 appLogin()이 동작하지 않으므로 이 엔드포인트를 사용합니다.
 */
@Profile("dev")
@RestController
@RequestMapping("/api/v1/auth")
class DevLoginController(
    private val userRepository: UserRepository,
    private val jwtProvider: JwtProvider,
) {

    @PostMapping("/dev-login")
    fun devLogin(): ApiResponse<LoginResponse> {
        val user = userRepository.findByProviderAndProviderId(SocialProvider.TOSS, "DEV_USER")
            ?: userRepository.save(
                User(
                    provider = SocialProvider.TOSS,
                    providerId = "DEV_USER",
                )
            )

        val token = jwtProvider.createToken(user.id)
        return ApiResponse.ok(LoginResponse(token))
    }
}
