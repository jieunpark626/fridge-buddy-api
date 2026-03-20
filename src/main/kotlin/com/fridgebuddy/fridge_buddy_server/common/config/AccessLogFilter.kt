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
class AccessLogFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val start = System.currentTimeMillis()
        filterChain.doFilter(request, response)
        val elapsed = System.currentTimeMillis() - start

        if (request.requestURI.startsWith("/actuator")) return

        log.info("{} {} {} {}ms", request.method, request.requestURI, response.status, elapsed)
    }
}