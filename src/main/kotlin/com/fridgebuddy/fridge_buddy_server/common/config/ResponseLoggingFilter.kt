package com.fridgebuddy.fridge_buddy_server.common.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Int.MAX_VALUE)
class ResponseLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        filterChain.doFilter(request, response)
        log.debug("RESPONSE: {} {} {} {}", response.status, request.method, request.requestURI,
            request.remoteAddr?.let { "client=$it" } ?: "")
    }
}