package com.deundeun.global.common;

import com.deundeun.global.exception.ApiException;
import com.deundeun.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private int status;
    private String code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, 200, "SUCCESS", null, data);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, 200, "SUCCESS", message, data);
    }

    public static ApiResponse<Void> fail(ApiException e) {
        ErrorCode errorCode = e.getErrorCode();
        return new ApiResponse<>(false, errorCode.getStatus().value(), errorCode.getCode(), e.getMessage(), null);
    }
}