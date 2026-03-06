package com.fridgebuddy.fridge_buddy_server.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.fridgebuddy.fridge_buddy_server.common.response.ApiResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtProvider: JwtProvider,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = resolveToken(request)

        if (token != null) {
            when (jwtProvider.validate(token)) {
                TokenStatus.VALID -> {
                    val userId = jwtProvider.getUserId(token)
                    val auth = UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        listOf(SimpleGrantedAuthority("ROLE_USER")),
                    )
                    SecurityContextHolder.getContext().authentication = auth
                }
                TokenStatus.EXPIRED -> {
                    writeUnauthorized(response, "토큰이 만료되었습니다. 다시 로그인해주세요.")
                    return
                }
                TokenStatus.INVALID -> {
                    writeUnauthorized(response, "유효하지 않은 토큰입니다.")
                    return
                }
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? =
        request.cookies?.find { it.name == "access_token" }?.value

    private fun writeUnauthorized(response: HttpServletResponse, message: String) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        objectMapper.writeValue(response.writer, ApiResponse.error(message))
    }
}
