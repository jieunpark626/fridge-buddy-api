package com.fridgebuddy.fridge_buddy_server.fridge.controller

import com.fridgebuddy.fridge_buddy_server.common.response.ApiResponse
import com.fridgebuddy.fridge_buddy_server.fridge.dto.AddFridgeItemRequest
import com.fridgebuddy.fridge_buddy_server.fridge.dto.FridgeItemDetailResponse
import com.fridgebuddy.fridge_buddy_server.fridge.dto.FridgeItemSummaryResponse
import com.fridgebuddy.fridge_buddy_server.fridge.dto.UpdateQuantityRequest
import com.fridgebuddy.fridge_buddy_server.fridge.service.FridgeService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/fridge")
class FridgeController(
    private val fridgeService: FridgeService,
) {

    /** 냉장고 재료 추가 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun addItem(
        @AuthenticationPrincipal userId: Long,
        @RequestBody request: AddFridgeItemRequest,
    ): ApiResponse<FridgeItemDetailResponse> =
        ApiResponse.ok(fridgeService.addItem(userId, request))

    /** 냉장고 재료 목록 조회 (유통기한 임박순) */
    @GetMapping
    fun getItems(@AuthenticationPrincipal userId: Long): ApiResponse<List<FridgeItemSummaryResponse>> =
        ApiResponse.ok(fridgeService.getItems(userId))

    /** 냉장고 재료 상세 조회 */
    @GetMapping("/{id}")
    fun getItem(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long,
    ): ApiResponse<FridgeItemDetailResponse> =
        ApiResponse.ok(fridgeService.getItem(userId, id))

    /** 수량 변경 */
    @PatchMapping("/{id}/quantity")
    fun updateQuantity(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long,
        @RequestBody request: UpdateQuantityRequest,
    ): ApiResponse<FridgeItemSummaryResponse> =
        ApiResponse.ok(fridgeService.updateQuantity(userId, id, request))

    /** 냉장고 재료 삭제 */
    @DeleteMapping("/{id}")
    fun deleteItem(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long,
    ): ApiResponse<Nothing?> {
        fridgeService.deleteItem(userId, id)
        return ApiResponse.ok(null)
    }
}
