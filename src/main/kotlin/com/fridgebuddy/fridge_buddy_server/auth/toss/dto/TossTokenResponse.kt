package com.fridgebuddy.fridge_buddy_server.auth.toss.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TossTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val scope: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TossTokenApiResponse(
    val resultType: String,
    val success: TossTokenResponse? = null,
    val error: TossApiError? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TossApiError(
    val code: String? = null,
    val message: String? = null,
)
