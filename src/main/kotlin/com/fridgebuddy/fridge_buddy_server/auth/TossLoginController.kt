package com.fridgebuddy.fridge_buddy_server.auth

import com.fridgebuddy.fridge_buddy_server.auth.dto.LoginResponse
import com.fridgebuddy.fridge_buddy_server.auth.toss.TossLoginService
import com.fridgebuddy.fridge_buddy_server.auth.toss.dto.TossLoginRequest
import com.fridgebuddy.fridge_buddy_server.common.response.ApiResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth/toss")
class TossLoginController(
    private val tossLoginService: TossLoginService,
) {

    /**
     * 토스 로그인 — authorizationCode를 교환해 JWT 토큰을 응답 바디로 반환
     * POST /api/v1/auth/toss/login
     *
     * iOS WKWebView 서드파티 쿠키 차단 정책으로 인해 쿠키 대신 바디로 토큰을 반환합니다.
     * 클라이언트는 토큰을 localStorage에 저장하고 이후 요청에 Authorization: Bearer 헤더로 전송합니다.
     */
    @PostMapping("/login")
    fun login(
        @RequestBody request: TossLoginRequest,
    ): ApiResponse<LoginResponse> {
        val token = tossLoginService.login(request.authorizationCode, request.referrer)
        return ApiResponse.ok(LoginResponse(token))
    }

    /**
     * 토스 연결 끊기 콜백 수신
     * POST /api/v1/auth/toss/unlink-callback
     *
     * 콘솔에서 등록한 콜백 URL로 토스가 호출합니다.
     * userKey로 유저 및 연관 데이터(냉장고 아이템) 삭제
     */
    @PostMapping("/unlink-callback")
    fun unlinkCallback(@RequestBody body: Map<String, Any>): ApiResponse<Nothing?> {
        val userKey = (body["userKey"] as? Number)?.toLong()
        if (userKey != null) {
            tossLoginService.unlinkUser(userKey)
        }
        return ApiResponse.ok(null)
    }

}
