package com.fridgebuddy.fridge_buddy_server.ingredient.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fridgebuddy.fridge_buddy_server.common.exception.InvalidIngredientException
import com.fridgebuddy.fridge_buddy_server.ingredient.dto.IngredientAiDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class IngredientAiService(
    private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    private val ingredientAiSaver: IngredientAiSaver,
    @Value("\${claude.api.key}") private val apiKey: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 키워드가 보관 가능한 식재료인지 동기적으로 판단.
     * 유효하지 않으면 InvalidIngredientException을 던진다.
     */
    fun validate(keyword: String) {
        val request = mapOf(
            "model" to "claude-haiku-4-5-20251001",
            "max_tokens" to 100,
            "messages" to listOf(
                mapOf("role" to "user", "content" to buildValidationPrompt(keyword))
            ),
        )

        val response = callApi(request)
        val json = stripCodeFence(response.content.first().text)
        val node = objectMapper.readTree(json)

        if (!node.path("valid").asBoolean(true)) {
            val reason = node.path("reason").asText("식재료로 적합하지 않습니다.")
            throw InvalidIngredientException(reason)
        }
    }

    /**
     * DB에 재료가 없을 때만 호출.
     * PENDING 선점 저장은 호출 전에 완료된 상태여야 함.
     */
    @Async("aiTaskExecutor")
    fun generateAndSave(ingredientId: Long, keyword: String) {
        log.info("AI 재료 생성 시작: keyword=$keyword, id=$ingredientId")
        try {
            val aiData = callClaude(keyword)
            ingredientAiSaver.save(ingredientId, aiData, keyword)
            log.info("AI 재료 생성 완료: keyword=$keyword → name=${aiData.name}")
        } catch (e: Exception) {
            log.error("AI 재료 생성 실패: keyword=$keyword, id=$ingredientId", e)
            ingredientAiSaver.markFailed(ingredientId)
        }
    }

    private fun callClaude(keyword: String): IngredientAiDto {
        val request = mapOf(
            "model" to "claude-haiku-4-5-20251001",
            "max_tokens" to 1024,
            "messages" to listOf(
                mapOf("role" to "user", "content" to buildGenerationPrompt(keyword))
            ),
        )

        val response = callApi(request)
        val json = stripCodeFence(response.content.first().text)
        return objectMapper.readValue(json, IngredientAiDto::class.java)
    }

    private fun callApi(request: Map<String, Any>): ClaudeApiResponse {
        return restClient.post()
            .uri("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(ClaudeApiResponse::class.java)
            ?: throw IllegalStateException("Claude API 응답이 비어있습니다.")
    }

    private fun stripCodeFence(raw: String): String = raw.trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()

    private fun buildValidationPrompt(keyword: String): String = """
        다음 입력이 냉장고나 실온에 보관하는 실제 식재료(음식 재료)인지 판단하세요.
        JSON만 반환하세요. 다른 텍스트는 절대 포함하지 마세요.

        유효하지 않은 경우:
        - 음식 재료가 아닌 것 (예: 가방, 책, 숟가락)
        - 특정 브랜드·음식점명 (예: 엽떡, 배달의민족, 맥도날드)
        - 일반 음식 종류(떡볶이, 치킨 등)는 유효함

        응답 형식:
        {"valid": true} 또는 {"valid": false, "reason": "한 문장 이유"}

        입력: $keyword
    """.trimIndent()

    private fun buildGenerationPrompt(keyword: String): String = """
        당신은 식재료 보관법 전문가입니다.
        다음 식재료에 대한 정보를 아래 JSON 형식으로만 응답하세요.
        JSON 외의 다른 텍스트(설명, 마크다운 코드블록 등)는 절대 포함하지 마세요.

        식재료: $keyword

        응답 형식:
        {
          "name": "정규 재료명 (한국어 표준명, 예: 달걀)",
          "icon": "재료를 가장 잘 나타내는 이모지 1개 (예: 🥚)",
          "category": "MEAT, SEAFOOD, VEGETABLE, FRUIT, DAIRY, EGG, GRAIN, SEASONING, PROCESSED, BEVERAGE 중 정확히 하나",
          "defaultStorageType": "FRIDGE, FREEZER, ROOM_TEMP 중 정확히 하나",
          "shelfLifeText": "보관 기간 표시, 예: '냉장 7일', '개봉 후 3일'",
          "storageMethod": "보관 방법 1~2문장 핵심 요약",
          "extraInfo": "알고 계셨나요? 흥미로운 사실 한 문장. 없으면 null",
          "aliases": ["동의어", "영문명", "다른 표기법"],
          "tips": [
            {"stepOrder": 1, "content": "구체적인 보관 팁"},
            {"stepOrder": 2, "content": "구체적인 보관 팁"},
            {"stepOrder": 3, "content": "구체적인 보관 팁"}
          ],
          "cautions": [
            {"content": "주의사항 내용", "cautionType": "WARNING 또는 CAUTION"}
          ]
        }
    """.trimIndent()

    private data class ClaudeApiResponse(val content: List<ContentBlock>) {
        data class ContentBlock(val type: String, val text: String)
    }
}