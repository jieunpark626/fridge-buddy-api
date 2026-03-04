package com.fridgebuddy.fridge_buddy_server.fridge.controller

import com.fridgebuddy.fridge_buddy_server.common.response.ApiResponse
import com.fridgebuddy.fridge_buddy_server.fridge.dto.AddFridgeItemRequest
import com.fridgebuddy.fridge_buddy_server.fridge.dto.FridgeItemDetailResponse
import com.fridgebuddy.fridge_buddy_server.fridge.dto.FridgeItemSummaryResponse
import com.fridgebuddy.fridge_buddy_server.fridge.dto.UpdateQuantityRequest
import com.fridgebuddy.fridge_buddy_server.fridge.service.FridgeService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/fridge")
class FridgeController(
    private val fridgeService: FridgeService,
) {

    /** 냉장고 재료 추가 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun addItem(@RequestBody request: AddFridgeItemRequest): ApiResponse<FridgeItemDetailResponse> =
        ApiResponse.ok(fridgeService.addItem(request))

    /** 냉장고 재료 목록 조회 (유통기한 임박순) */
    @GetMapping
    fun getItems(): ApiResponse<List<FridgeItemSummaryResponse>> =
        ApiResponse.ok(fridgeService.getItems())

    /** 냉장고 재료 상세 조회 */
    @GetMapping("/{id}")
    fun getItem(@PathVariable id: Long): ApiResponse<FridgeItemDetailResponse> =
        ApiResponse.ok(fridgeService.getItem(id))

    /** 수량 변경 */
    @PatchMapping("/{id}/quantity")
    fun updateQuantity(
        @PathVariable id: Long,
        @RequestBody request: UpdateQuantityRequest,
    ): ApiResponse<FridgeItemSummaryResponse> =
        ApiResponse.ok(fridgeService.updateQuantity(id, request))

    /** 냉장고 재료 삭제 */
    @DeleteMapping("/{id}")
    fun deleteItem(@PathVariable id: Long): ApiResponse<Nothing?> {
        fridgeService.deleteItem(id)
        return ApiResponse.ok(null)
    }
}