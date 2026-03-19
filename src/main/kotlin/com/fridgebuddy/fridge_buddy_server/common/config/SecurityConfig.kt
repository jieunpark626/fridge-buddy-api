package com.fridgebuddy.fridge_buddy_server.common.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fridgebuddy.fridge_buddy_server.auth.JwtAuthFilter
import com.fridgebuddy.fridge_buddy_server.common.response.ApiResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter,
    private val objectMapper: ObjectMapper,
) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOrigins = listOf(
            "https://fridgebuddy.apps.tossmini.com",       // 라이브
            "https://fridgebuddy.private-apps.tossmini.com", // 콘솔 QR 테스트
            "http://localhost:3000",                         // 로컬 개발
        )
        config.allowedMethods = listOf("GET", "POST", "PATCH", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("*")
        config.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                    .requestMatchers("/api/v1/ingredients/**").permitAll()
                    .requestMatchers("/api/v1/auth/logout").permitAll()
                    .requestMatchers("/api/v1/auth/toss/login").permitAll()
                    .requestMatchers("/api/v1/auth/toss/unlink-callback").permitAll()
                    .requestMatchers("/api/v1/auth/dev-login").permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint { _, response, _ ->
                    response.status = 401
                    response.contentType = MediaType.APPLICATION_JSON_VALUE
                    response.characterEncoding = "UTF-8"
                    objectMapper.writeValue(response.writer, ApiResponse.error("로그인이 필요합니다."))
                }
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
