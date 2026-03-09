package com.fridgebuddy.fridge_buddy_server.auth.toss.dto

data class TossLoginRequest(
    val authorizationCode: String,
    val referrer: String,
)
