package com.fridgebuddy.fridge_buddy_server.common.response

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
) {
    companion object {
        fun <T> ok(data: T) = ApiResponse(success = true, data = data)
        fun error(message: String) = ApiResponse<Nothing>(success = false, message = message)
    }
}