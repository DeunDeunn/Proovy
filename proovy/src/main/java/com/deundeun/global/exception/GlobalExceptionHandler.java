package com.deundeun.global.exception;

import com.deundeun.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException e) {
        ErrorCode errorCode = e.getErrorCode();

        log.warn("ApiException: {} - {}", errorCode.getCode(), e.getMessage(), e);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(e));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.INVALID_REQUEST.getMessage());

        log.warn("Validation failed: {}", message);

        ApiException wrapped = new ApiException(ErrorCode.INVALID_REQUEST, message);
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getStatus())
                .body(ApiResponse.fail(wrapped));
    }
    // 요청 body의 JSON이 깨졌거나(문법 오류), 타입이 안 맞을 때 (예: "age": "abc")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("Invalid JSON request body: {}", e.getMessage());

        ApiException wrapped = new ApiException(ErrorCode.INVALID_JSON_FORMAT);
        return ResponseEntity
                .status(ErrorCode.INVALID_JSON_FORMAT.getStatus())
                .body(ApiResponse.fail(wrapped));
    }

    // DB 조회/저장 중 발생하는 예외 (제약조건 위반, 커넥션 오류 등)의 상위 타입
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccessException(DataAccessException e) {
        log.error("Database access error", e);

        ApiException wrapped = new ApiException(ErrorCode.DATABASE_ERROR);
        return ResponseEntity
                .status(ErrorCode.DATABASE_ERROR.getStatus())
                .body(ApiResponse.fail(wrapped));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);

        ApiException wrapped = new ApiException(ErrorCode.SERVER_ERROR);

        return ResponseEntity
                .status(ErrorCode.SERVER_ERROR.getStatus())
                .body(ApiResponse.fail(wrapped));
    }
}
