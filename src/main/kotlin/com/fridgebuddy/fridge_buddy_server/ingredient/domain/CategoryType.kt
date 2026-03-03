package com.fridgebuddy.fridge_buddy_server.ingredient.domain

enum class CategoryType(val label: String) {
    MEAT("육류"),
    SEAFOOD("해산물"),
    VEGETABLE("채소"),
    FRUIT("과일"),
    DAIRY("유제품"),
    EGG("계란/알류"),
    GRAIN("곡류"),
    SEASONING("양념/조미료"),
    PROCESSED("가공식품"),
    BEVERAGE("음료"),
}