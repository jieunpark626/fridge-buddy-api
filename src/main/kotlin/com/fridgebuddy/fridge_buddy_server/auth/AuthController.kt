package com.fridgebuddy.fridge_buddy_server.auth

import com.fridgebuddy.fridge_buddy_server.auth.toss.TossLoginService
import com.fridgebuddy.fridge_buddy_server.common.response.ApiResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val tossLoginService: TossLoginService,
) {

    /**
     * 로그아웃 — Toss AccessToken 무효화 + 클라이언트 localStorage 삭제
     *
     * 유효한 JWT가 있으면 Toss 측 토큰도 revoke한다.
     * 토큰이 없거나 만료된 경우에는 클라이언트가 알아서 localStorage를 비우면 된다.
     */
    @PostMapping("/logout")
    fun logout(): ApiResponse<Nothing?> {
        val userId = SecurityContextHolder.getContext().authentication?.principal as? Long
        if (userId != null) {
            tossLoginService.logout(userId)
        }
        return ApiResponse.ok(null)
    }
}