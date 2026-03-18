package com.fridgebuddy.fridge_buddy_server.ingredient.controller

import com.fridgebuddy.fridge_buddy_server.common.response.ApiResponse
import com.fridgebuddy.fridge_buddy_server.ingredient.domain.IngredientStatus
import com.fridgebuddy.fridge_buddy_server.ingredient.dto.IngredientDetailResponse
import com.fridgebuddy.fridge_buddy_server.ingredient.dto.IngredientResponse
import com.fridgebuddy.fridge_buddy_server.ingredient.dto.IngredientStatusResponse
import com.fridgebuddy.fridge_buddy_server.ingredient.service.IngredientService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/ingredients")
class IngredientController(
    private val ingredientService: IngredientService,
) {

    /** 전체 조회 또는 부분 일치 검색 (COMPLETED 재료만) */
    @GetMapping
    fun search(@RequestParam(required = false) name: String?): ApiResponse<List<IngredientResponse>> =
        ApiResponse.ok(ingredientService.search(name))

    /**
     * AI 재료 생성 요청 (이미 존재하면 즉시 반환)
     * - 200: 즉시 반환 (COMPLETED)
     * - 202: AI 생성 중 → id로 폴링 시작
     */
    @PostMapping("/generate")
    fun generate(@RequestBody body: Map<String, String>): ResponseEntity<ApiResponse<IngredientStatusResponse>> {
        val keyword = requireNotNull(body["keyword"]) { "keyword는 필수입니다." }
        val result = ingredientService.generate(keyword)
        val httpStatus = if (result.status == IngredientStatus.PENDING) HttpStatus.ACCEPTED else HttpStatus.OK
        return ResponseEntity.status(httpStatus).body(ApiResponse.ok(result))
    }

    /**
     * 폴링 엔드포인트: 클라이언트가 3초마다 호출
     * - 200: COMPLETED (data 포함) 또는 FAILED
     * - 202: 아직 PENDING
     */
    @GetMapping("/{id}/status")
    fun getStatus(@PathVariable id: Long): ResponseEntity<ApiResponse<IngredientStatusResponse>> {
        val result = ingredientService.getStatusById(id)
        val httpStatus = if (result.status == IngredientStatus.PENDING) HttpStatus.ACCEPTED else HttpStatus.OK
        return ResponseEntity.status(httpStatus).body(ApiResponse.ok(result))
    }

    /** ID로 완성된 재료 상세 조회 */
    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ApiResponse<IngredientDetailResponse> =
        ApiResponse.ok(ingredientService.getById(id))
}