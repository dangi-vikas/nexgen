package com.nexgen.cart_service.dto;

import java.time.Instant;
import java.util.List;

public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private List<String> errors;
    private Instant timestamp;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, String message, T data, List<String> errors, Instant timestamp) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.errors = errors;
        this.timestamp = timestamp;
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<T>(true, message, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Request processed successfully.");
    }

    public static ApiResponse<?> error(String message) {
        return error(message, null);
    }

    public static ApiResponse<?> error(String message, List<String> errors) {
        return new ApiResponse<Object>(false, message, null, errors, Instant.now());
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
