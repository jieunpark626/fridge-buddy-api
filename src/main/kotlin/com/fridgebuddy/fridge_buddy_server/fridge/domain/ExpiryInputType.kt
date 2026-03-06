package com.fridgebuddy.fridge_buddy_server.fridge.domain

enum class ExpiryInputType {
    MANUAL,  // 사용자가 직접 날짜 입력
    AUTO,    // 구매일 + 재료 유통기한(일수) 자동 계산
}