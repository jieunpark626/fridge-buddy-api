package com.fridgebuddy.fridge_buddy_server.auth.toss

import com.fridgebuddy.fridge_buddy_server.auth.toss.dto.TossTokenApiResponse
import com.fridgebuddy.fridge_buddy_server.auth.toss.dto.TossTokenResponse
import com.fridgebuddy.fridge_buddy_server.auth.toss.dto.TossUserInfoApiResponse
import com.fridgebuddy.fridge_buddy_server.auth.toss.dto.TossUserInfoResponse
import com.fridgebuddy.fridge_buddy_server.common.exception.TossApiException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

@Component
class TossPartnerApiClient(
    @Value("\${toss.api.base-url:https://apps-in-toss-api.toss.im}") private val baseUrl: String,
    @Value("\${toss.mtls.cert-path:}") private val certPath: String,
    @Value("\${toss.mtls.key-path:}") private val keyPath: String,
) {
    private val client: RestClient by lazy { buildClient() }

    private fun buildClient(): RestClient {
        val builder = RestClient.builder().baseUrl(baseUrl)

        if (certPath.isNotBlank() && keyPath.isNotBlank()) {
            val sslContext = buildMtlsSslContext()
            val httpClient = HttpClient.newBuilder().sslContext(sslContext).build()
            builder.requestFactory(JdkClientHttpRequestFactory(httpClient))
        }

        return builder.build()
    }

    /**
     * PEM 형식의 인증서(CERTIFICATE)와 PKCS8 형식의 개인키(PRIVATE KEY)를 읽어
     * mTLS용 SSLContext를 생성합니다.
     *
     * 주의: RSA PRIVATE KEY(PKCS1) 형식의 키는 지원하지 않습니다.
     *       openssl pkcs8 -topk8 -nocrypt -in key.pem -out key.pk8.pem 으로 변환하세요.
     */
    private fun buildMtlsSslContext(): SSLContext {
        val certBytes = Files.readAllBytes(Paths.get(certPath))
        val cert = CertificateFactory.getInstance("X.509")
            .generateCertificate(certBytes.inputStream())

        val keyPem = String(Files.readAllBytes(Paths.get(keyPath)))
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\r", "").replace("\n", "").trim()
        val keyBytes = Base64.getDecoder().decode(keyPem)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)

        val privateKey = runCatching { KeyFactory.getInstance("RSA").generatePrivate(keySpec) }
            .getOrElse { KeyFactory.getInstance("EC").generatePrivate(keySpec) }

        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(null, null)
        keyStore.setKeyEntry("toss-mtls", privateKey, CharArray(0), arrayOf(cert))

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, CharArray(0))

        return SSLContext.getInstance("TLS").also { it.init(kmf.keyManagers, null, null) }
    }

    /**
     * authorizationCode → AccessToken 교환
     * POST /api-partner/v1/apps-in-toss/user/oauth2/generate-token
     */
    fun generateToken(authorizationCode: String, referrer: String): TossTokenResponse {
        val response = client.post()
            .uri("/api-partner/v1/apps-in-toss/user/oauth2/generate-token")
            .body(mapOf("authorizationCode" to authorizationCode, "referrer" to referrer))
            .retrieve()
            .body(TossTokenApiResponse::class.java)
            ?: error("토스 generate-token 응답이 비어있습니다.")
        return response.success
            ?: throw TossApiException("토스 generate-token 실패: [${response.error?.code}] ${response.error?.message}")
    }

    /**
     * AccessToken → 사용자 정보 조회
     * GET /api-partner/v1/apps-in-toss/user/oauth2/login-me
     */
    fun getLoginMe(accessToken: String): TossUserInfoResponse {
        val response = client.get()
            .uri("/api-partner/v1/apps-in-toss/user/oauth2/login-me")
            .header("Authorization", "Bearer $accessToken")
            .retrieve()
            .body(TossUserInfoApiResponse::class.java)
            ?: error("토스 login-me 응답이 비어있습니다.")
        return response.success
            ?: throw TossApiException("토스 login-me 실패: [${response.error?.code}] ${response.error?.message}")
    }

    /**
     * userKey에 연결된 모든 AccessToken 무효화
     * POST /api-partner/v1/apps-in-toss/user/oauth2/access/remove-by-user-key
     *
     * 주의: AccessToken이 많으면 readTimeout(3초)이 발생할 수 있음. 재시도 금지.
     */
    fun removeByUserKey(userKey: Long) {
        client.post()
            .uri("/api-partner/v1/apps-in-toss/user/oauth2/access/remove-by-user-key")
            .body(mapOf("userKey" to userKey))
            .retrieve()
            .toBodilessEntity()
    }
}
