package com.fridgebuddy.fridge_buddy_server.ingredient.domain

enum class IngredientStatus {
    PENDING,    // AI 생성 진행 중
    COMPLETED,  // 정상 등록 완료
    FAILED,     // AI 생성 실패
}
