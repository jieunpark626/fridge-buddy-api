package com.fridgebuddy.fridge_buddy_server.ingredient.service

import com.fasterxml.jackson.databind.ObjectMapper
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
                mapOf("role" to "user", "content" to buildPrompt(keyword))
            ),
        )

        val response = restClient.post()
            .uri("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(ClaudeApiResponse::class.java)
            ?: throw IllegalStateException("Claude API 응답이 비어있습니다.")

        val raw = response.content.first().text.trim()
        // Claude가 ```json ... ``` 으로 감싸는 경우 코드펜스 제거
        val json = raw
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        return objectMapper.readValue(json, IngredientAiDto::class.java)
    }

    private fun buildPrompt(keyword: String): String = """
        당신은 식재료 보관법 전문가입니다.
        다음 식재료에 대한 정보를 아래 JSON 형식으로만 응답하세요.
        JSON 외의 다른 텍스트(설명, 마크다운 코드블록 등)는 절대 포함하지 마세요.

        식재료: $keyword

        응답 형식:
        {
          "name": "정규 재료명 (한국어 표준명, 예: 달걀)",
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