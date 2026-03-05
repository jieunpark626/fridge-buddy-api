package com.fridgebuddy.fridge_buddy_server.auth

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date

enum class TokenStatus { VALID, EXPIRED, INVALID }

@Component
class JwtProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration-ms}") private val expirationMs: Long,
) {
    private val key by lazy { Keys.hmacShaKeyFor(secret.toByteArray()) }

    fun createToken(userId: Long): String =
        Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)
            .compact()

    fun getUserId(token: String): Long =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
            .subject
            .toLong()

    fun validate(token: String): TokenStatus =
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
            TokenStatus.VALID
        } catch (e: ExpiredJwtException) {
            TokenStatus.EXPIRED
        } catch (e: JwtException) {
            TokenStatus.INVALID
        } catch (e: IllegalArgumentException) {
            TokenStatus.INVALID
        }
}
