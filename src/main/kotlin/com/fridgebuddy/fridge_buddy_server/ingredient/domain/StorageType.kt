package com.fridgebuddy.fridge_buddy_server.ingredient.domain

enum class StorageType(val label: String) {
    FRIDGE("냉장"),
    FREEZER("냉동"),
    ROOM_TEMP("실온"),
}