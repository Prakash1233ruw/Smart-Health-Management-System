package com.pp.smarthealth.exception;

import org.springframework.http.HttpStatus;

public class AIServiceException extends RuntimeException {
    private final HttpStatus status;

    private AIServiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
    public static AIServiceException unavailable() {
        return new AIServiceException(HttpStatus.SERVICE_UNAVAILABLE, "AI analysis is currently unavailable.");
    }
    public static AIServiceException missingKey() {
        return new AIServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                "Gemini API key is not configured. Set AI_API_KEY in the application's run environment and restart.");
    }
    public static AIServiceException quotaExceeded() {
        return new AIServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                "Gemini usage limit reached. Please try again later or check your free-tier quota.");
    }
    public static AIServiceException configurationRejected() {
        return new AIServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                "Gemini rejected the request. Check the server's Gemini API key, model, and project access.");
    }
    public static AIServiceException timeout() {
        return new AIServiceException(HttpStatus.GATEWAY_TIMEOUT, "AI analysis timed out. Please try again later.");
    }
    public static AIServiceException invalidResponse() {
        return new AIServiceException(HttpStatus.BAD_GATEWAY, "AI analysis returned an invalid or incomplete response.");
    }
    public static AIServiceException historyUnavailable() {
        return new AIServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Analysis history could not be saved.");
    }
}
