package com.fridgebuddy.fridge_buddy_server.fridge.dto

import java.math.BigDecimal

data class UpdateQuantityRequest(
    val quantity: BigDecimal,
)