package org.gripday.bookstore.domain.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ErrorResponse(
    String code,
    String message,
    String details,
    Instant timestamp,
    String path,
    String method,
    String correlationId,
    String requestId,
    List<FieldError> fields
) {
    
    public record FieldError(
        String field,
        Object rejectedValue,
        String message
    ) {}
    
    public static ErrorResponse of(String code, String message, String details) {
        return new ErrorResponse(
            code,
            message,
            details,
            Instant.now(),
            null,
            null,
            null,
            null,
            null
        );
    }
    
    public static ErrorResponse of(String code, String message, String details, String path, String method) {
        return new ErrorResponse(
            code,
            message,
            details,
            Instant.now(),
            path,
            method,
            null,
            null,
            null
        );
    }
    
    public ErrorResponse withCorrelationId(String correlationId) {
        return new ErrorResponse(
            this.code,
            this.message,
            this.details,
            this.timestamp,
            this.path,
            this.method,
            correlationId,
            this.requestId,
            this.fields
        );
    }
    
    public ErrorResponse withRequestId(String requestId) {
        return new ErrorResponse(
            this.code,
            this.message,
            this.details,
            this.timestamp,
            this.path,
            this.method,
            this.correlationId,
            requestId,
            this.fields
        );
    }
    
    public ErrorResponse withFields(List<FieldError> fields) {
        return new ErrorResponse(
            this.code,
            this.message,
            this.details,
            this.timestamp,
            this.path,
            this.method,
            this.correlationId,
            this.requestId,
            fields
        );
    }
}