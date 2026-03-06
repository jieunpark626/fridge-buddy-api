package com.fridgebuddy.fridge_buddy_server.common.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fridgebuddy.fridge_buddy_server.auth.JwtAuthFilter
import com.fridgebuddy.fridge_buddy_server.auth.OAuth2FailureHandler
import com.fridgebuddy.fridge_buddy_server.auth.OAuth2SuccessHandler
import com.fridgebuddy.fridge_buddy_server.common.response.ApiResponse
import com.fridgebuddy.fridge_buddy_server.user.service.CustomOAuth2UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter,
    private val customOAuth2UserService: CustomOAuth2UserService,
    private val oAuth2SuccessHandler: OAuth2SuccessHandler,
    private val oAuth2FailureHandler: OAuth2FailureHandler,
    private val objectMapper: ObjectMapper,
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/v1/ingredients/**").permitAll()
                    .requestMatchers("/api/v1/auth/logout").permitAll()
                    .requestMatchers("/oauth2/authorization/**").permitAll()
                    .requestMatchers("/login/oauth2/code/**").permitAll()
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
            .oauth2Login { oauth2 ->
                oauth2
                    .userInfoEndpoint { it.userService(customOAuth2UserService) }
                    .successHandler(oAuth2SuccessHandler)
                    .failureHandler(oAuth2FailureHandler)
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
