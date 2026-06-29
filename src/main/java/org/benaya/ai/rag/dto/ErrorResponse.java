package org.benaya.ai.rag.dto;

public record ErrorResponse(
        int status,
        String error,
        String message
) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message);
    }
}
