package com.fridgebuddy.fridge_buddy_server.common.exception

import com.fridgebuddy.fridge_buddy_server.common.response.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResource(e: NoResourceFoundException) =
        ApiResponse.error("요청한 리소스를 찾을 수 없습니다.")

    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotAllowed(e: HttpRequestMethodNotSupportedException) =
        ApiResponse.error("지원하지 않는 HTTP 메서드입니다: ${e.method}")


    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(InvalidIngredientException::class)
    fun handleInvalidIngredient(e: InvalidIngredientException) =
        ApiResponse.error(e.message ?: "식재료로 적합하지 않습니다.")

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(DuplicateFridgeItemException::class)
    fun handleDuplicate(e: DuplicateFridgeItemException) =
        ApiResponse.error(e.message ?: "이미 등록된 항목입니다.")

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(TossApiException::class)
    fun handleTossApiError(e: TossApiException) =
        ApiResponse.error(e.message ?: "토스 인증에 실패했습니다.")

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(e: NoSuchElementException) =
        ApiResponse.error(e.message ?: "리소스를 찾을 수 없습니다.")

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException) =
        ApiResponse.error(e.message ?: "잘못된 요청입니다.")

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception::class)
    fun handleInternalError(e: Exception): Any {
        log.error("Unhandled exception", e)
        return ApiResponse.error("서버 내부 오류가 발생했습니다.")
    }
}