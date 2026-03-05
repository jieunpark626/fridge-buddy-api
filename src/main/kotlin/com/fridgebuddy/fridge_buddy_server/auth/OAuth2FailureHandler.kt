package com.fridgebuddy.fridge_buddy_server.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.stereotype.Component

@Component
class OAuth2FailureHandler(
    @Value("\${oauth2.redirect-uri}") private val redirectUri: String,
) : SimpleUrlAuthenticationFailureHandler() {

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        redirectStrategy.sendRedirect(request, response, "$redirectUri?error=login_failed")
    }
}
