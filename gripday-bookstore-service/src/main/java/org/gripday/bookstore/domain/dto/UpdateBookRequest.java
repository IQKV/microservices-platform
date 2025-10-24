package org.gripday.bookstore.domain.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateBookRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    String title,
    
    @NotBlank(message = "Author is required")
    @Size(max = 255, message = "Author must not exceed 255 characters")
    String author,
    
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    String description,
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @DecimalMax(value = "9999.99", message = "Price must not exceed 9999.99")
    BigDecimal price,
    
    @NotNull(message = "Category ID is required")
    @Positive(message = "Category ID must be positive")
    Long categoryId
) {}