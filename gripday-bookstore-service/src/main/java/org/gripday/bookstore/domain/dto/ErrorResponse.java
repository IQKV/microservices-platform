package org.gripday.bookstore.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Standard error response format")
public record ErrorResponse(
    @Schema(description = "Error code identifying the type of error", example = "RESOURCE_NOT_FOUND")
    String code,
    
    @Schema(description = "Human-readable error message", example = "Book not found with ID: 123")
    String message,
    
    @Schema(description = "Additional details about the error", example = "The requested book could not be found")
    String details,
    
    @Schema(description = "Timestamp when the error occurred", example = "2024-01-15T10:30:00Z")
    Instant timestamp,
    
    @Schema(description = "Request path that caused the error", example = "/api/v1/bookstore/books/123")
    String path,
    
    @Schema(description = "HTTP method used", example = "GET")
    String method,
    
    @Schema(description = "Correlation ID for request tracing", example = "abc123-def456-ghi789")
    String correlationId,
    
    @Schema(description = "Unique request identifier", example = "req-001-2024")
    String requestId,
    
    @Schema(description = "Field-specific validation errors")
    List<FieldError> fields
) {
    
    @Schema(description = "Field-specific validation error")
    public record FieldError(
        @Schema(description = "Name of the field that failed validation", example = "title")
        String field,
        
        @Schema(description = "Value that was rejected", example = "")
        Object rejectedValue,
        
        @Schema(description = "Validation error message", example = "Title is required")
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