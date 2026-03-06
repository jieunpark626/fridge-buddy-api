package com.fridgebuddy.fridge_buddy_server.auth

import com.fridgebuddy.fridge_buddy_server.common.response.ApiResponse
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    @Value("\${cookie.secure}") private val cookieSecure: Boolean,
) {

    @PostMapping("/logout")
    fun logout(response: HttpServletResponse): ApiResponse<Nothing?> {
        val cookie = Cookie("access_token", "").apply {
            isHttpOnly = true
            secure = cookieSecure
            path = "/"
            maxAge = 0
        }
        response.addCookie(cookie)
        return ApiResponse.ok(null)
    }
}