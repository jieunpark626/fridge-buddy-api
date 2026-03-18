package com.fridgebuddy.fridge_buddy_server.ingredient.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fridgebuddy.fridge_buddy_server.common.exception.InvalidIngredientException
import com.fridgebuddy.fridge_buddy_server.ingredient.dto.IngredientAiDto
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.util.Optional
import java.util.concurrent.TimeUnit

@Service
class IngredientAiService(
    private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
    private val ingredientAiSaver: IngredientAiSaver,
    @Value("\${claude.api.key}") private val apiKey: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 정규화 결과 캐시: Optional.of(canonical) = 유효, Optional.empty() = 무효
    // 네트워크 오류 등 AI 호출 실패 시에는 캐싱하지 않음
    private val normalizeCache: Cache<String, Optional<String>> = Caffeine.newBuilder()
        .maximumSize(2000)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build()

    /**
     * 키워드가 보관 가능한 식재료인지 동기적으로 판단하고 정규명을 반환한다.
     * 유효하지 않으면 InvalidIngredientException을 던진다.
     * 정규명을 PENDING 저장에 사용하면 동의어 중복 저장을 방지할 수 있다.
     *
     * cache.get()을 사용하여 동일 keyword의 동시 요청 중 단 1개 스레드만 AI를 호출하고
     * 나머지는 결과를 기다린다 (thundering herd 방지).
     * 네트워크 오류 등 예외가 발생하면 캐싱하지 않아 다음 요청에서 재시도된다.
     */
    fun validateAndNormalize(keyword: String): String {
        val result = normalizeCache.get(keyword) { kw ->
            log.debug("[normalize] AI 호출: keyword={}", kw)
            val request = mapOf(
                "model" to "claude-haiku-4-5-20251001",
                "max_tokens" to 100,
                "messages" to listOf(
                    mapOf("role" to "user", "content" to buildValidationPrompt(kw))
                ),
            )
            val response = callApi(request)
            val json = stripCodeFence(response.content.first().text)
            val node = objectMapper.readTree(json)

            if (node.path("valid").asBoolean(true)) {
                Optional.of(node.path("canonical").asText(kw).trim().ifBlank { kw })
            } else {
                Optional.empty()
            }
        } ?: error("normalizeCache loader returned null for keyword=$keyword")

        log.debug("[normalize] 결과: keyword={}, valid={}", keyword, result.isPresent)
        return result.orElseThrow { InvalidIngredientException("식재료로 적합하지 않습니다.") }
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
        당신은 식재료 분류 전문가입니다.
        아래 입력이 냉장고나 실온에 보관 가능한 실제 식재료인지 판단하고, 유효하다면 한국어 표준 정규명을 반환하세요.
        정규명은 이후 DB 저장 및 중복 방지 키로 사용되므로 일관성이 중요합니다.

        <validity_rules>
        유효한 식재료: 날 재료, 가공 재료, 조미료, 일반 음식 종류(떡볶이, 치킨 등) 포함
        유효하지 않은 입력:
        - 음식 재료가 아닌 물건 (예: 가방, 책, 숟가락)
        - 특정 브랜드명·음식점명 (예: 엽떡, 배달의민족, 맥도날드 버거)
        </validity_rules>

        <canonical_rules>
        - 오타·비표준 표기 → 표준 한국어명으로 교정 (예: "브로컬리" → "브로콜리")
        - 영문 입력 → 한국어 표준명으로 변환 (예: "broccoli" → "브로콜리", "egg" → "달걀")
        - 이미 표준명이면 그대로 반환
        </canonical_rules>

        <examples>
        입력: "브로컬리" → {"valid": true, "canonical": "브로콜리"}
        입력: "egg" → {"valid": true, "canonical": "달걀"}
        입력: "떡볶이" → {"valid": true, "canonical": "떡볶이"}
        입력: "토마토소스" → {"valid": true, "canonical": "토마토소스"}
        입력: "맥도날드" → {"valid": false, "reason": "특정 브랜드명으로 식재료가 아닙니다."}
        입력: "숟가락" → {"valid": false, "reason": "식재료가 아닌 물건입니다."}
        </examples>

        응답은 아래 두 형식 중 하나의 JSON 객체만 출력하세요.
        {"valid": true, "canonical": "표준 정규명"} 또는 {"valid": false, "reason": "한 문장 이유"}

        입력: $keyword
    """.trimIndent()

    private fun buildGenerationPrompt(keyword: String): String = """
        당신은 한국 가정의 식재료 보관법 전문가입니다.
        실용적이고 정확한 보관 정보를 제공하여 사용자가 식재료를 올바르게 보관하고 낭비를 줄일 수 있도록 돕습니다.

        <task>
        아래 식재료에 대한 보관 정보를 JSON 형식으로 작성하세요.
        응답은 JSON 객체 하나만 출력하세요.
        </task>

        <writing_rules>
        모든 텍스트 필드는 존댓말(~하세요, ~합니다) 없이 간결한 명사형 또는 서술형으로 작성하세요.

        - storageMethod: 가장 중요한 보관 원칙 한 문장. 동사 어간형 또는 명사형으로 끝내세요.
          좋은 예: "키친타월로 감싸 밀폐 용기에 냉장 보관"
          나쁜 예: "키친타월로 감싸서 밀폐 용기에 넣어 냉장 보관하세요."

        - tips: storageMethod와 내용이 겹치지 않는 실천 팁 3개. 각 팁은 서로 다른 측면을 다루세요.
          다룰 수 있는 측면: 구매 시 선별법 / 전처리·손질법 / 보관 용기·포장 / 냉동 방법 / 신선도 확인법 / 해동·활용법
          각 팁은 명사형 또는 "~~ 선택", "~~ 가능", "~~ 보관" 형태의 한 문장으로 작성하세요.
          좋은 예: "꼭지가 초록색이고 단단한 것 선택"
          나쁜 예: "꼭지가 초록색이고 단단한 것을 고르세요."

        - cautions: 잘못 보관했을 때 발생하는 위험·손상 위주. 명사형 또는 "~~ 시 ~~ 발생" 형태의 한 문장.
          좋은 예: "물기 남은 채 보관 시 빠르게 무름"
          나쁜 예: "물기가 남은 채로 보관하면 빠르게 무릅니다."
        </writing_rules>

        <output_format>
        {
          "name": "한국어 표준 정규명 (예: '달걀', '브로콜리')",
          "icon": "재료를 가장 잘 표현하는 이모지 1개",
          "category": "MEAT | SEAFOOD | VEGETABLE | FRUIT | DAIRY | EGG | GRAIN | SEASONING | PROCESSED | BEVERAGE",
          "defaultStorageType": "FRIDGE | FREEZER | ROOM_TEMP",
          "shelfLifeText": "가능한 모든 보관 방법의 기간을 ' · ' 구분자로 연결 (예: '냉장 5일 · 냉동 3개월', '냉장 1주일 · 실온 3일'). 불가능한 보관 방법은 제외.",
          "shelfLifeDaysFridge": 냉장 보관 가능 일수 (정수, 불가하면 null),
          "shelfLifeDaysFreezer": 냉동 보관 가능 일수 (정수, 불가하면 null),
          "shelfLifeDaysRoomTemp": 실온 보관 가능 일수 (정수, 불가하면 null),
          "storageMethod": "핵심 보관 원칙 한 문장 (명사형 또는 동사 어간형)",
          "extraInfo": "사용자가 몰랐을 법한 흥미로운 사실 한 문장 (없으면 null)",
          "aliases": ["동의어", "영문명", "다른 표기법"],
          "tips": [
            {"stepOrder": 1, "content": "명사형 또는 '~~ 선택/가능/보관' 형태의 한 문장"},
            {"stepOrder": 2, "content": "tip 1과 다른 측면의 한 문장"},
            {"stepOrder": 3, "content": "tip 1, 2와 다른 측면의 한 문장"}
          ],
          "cautions": [
            {"content": "명사형 또는 '~~ 시 ~~ 발생' 형태의 한 문장", "cautionType": "WARNING | CAUTION"}
          ]
        }
        </output_format>

        식재료: $keyword
    """.trimIndent()

    private data class ClaudeApiResponse(val content: List<ContentBlock>) {
        data class ContentBlock(val type: String, val text: String)
    }
}