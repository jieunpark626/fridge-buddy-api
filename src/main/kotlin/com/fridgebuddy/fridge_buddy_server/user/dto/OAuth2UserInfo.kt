package com.fridgebuddy.fridge_buddy_server.user.dto

abstract class OAuth2UserInfo(val attributes: Map<String, Any>) {
    abstract val id: String
    abstract val nickname: String
    abstract val profileImage: String?
}

class NaverOAuth2UserInfo(attributes: Map<String, Any>) : OAuth2UserInfo(attributes) {
    private val response = attributes["response"] as? Map<*, *> ?: emptyMap<String, Any>()
    override val id: String = response["id"] as? String ?: ""
    override val nickname: String = response["nickname"] as? String ?: ""
    override val profileImage: String? = response["profile_image"] as? String
}
