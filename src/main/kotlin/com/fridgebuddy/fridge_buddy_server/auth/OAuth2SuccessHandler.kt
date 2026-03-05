package com.fridgebuddy.fridge_buddy_server.auth

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class OAuth2SuccessHandler(
    private val jwtProvider: JwtProvider,
    @Value("\${oauth2.redirect-uri}") private val redirectUri: String,
    @Value("\${cookie.secure}") private val cookieSecure: Boolean,
) : SimpleUrlAuthenticationSuccessHandler() {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val oAuth2User = authentication.principal as OAuth2User
        val userId = oAuth2User.attributes["userId"] as Long

        val token = jwtProvider.createToken(userId)

        val cookie = Cookie("access_token", token).apply {
            isHttpOnly = true
            secure = cookieSecure
            path = "/"
            maxAge = 24 * 60 * 60 // 24시간
        }
        response.addCookie(cookie)

        redirectStrategy.sendRedirect(request, response, redirectUri)
    }
}
