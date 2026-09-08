package com.example.activity.ai;

public class AiServiceException extends RuntimeException {

    private final int code;

    public AiServiceException(int code, String message) {
        super(message);
        this.code = code;
    }

    public AiServiceException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
