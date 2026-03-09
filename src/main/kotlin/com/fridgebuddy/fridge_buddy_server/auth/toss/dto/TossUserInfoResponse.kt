package com.fridgebuddy.fridge_buddy_server.auth.toss.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TossUserInfoResponse(
    val userKey: Long,
    val scope: String,
    val agreedTerms: List<String>? = null,
    val name: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TossUserInfoApiResponse(
    val resultType: String,
    val success: TossUserInfoResponse,
)
