package org.gripday.bookstore.domain.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateBookRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    String title,
    
    @NotBlank(message = "Author is required")
    @Size(max = 255, message = "Author must not exceed 255 characters")
    String author,
    
    @NotBlank(message = "ISBN is required")
    @Pattern(regexp = "^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$", 
             message = "Invalid ISBN format")
    String isbn,
    
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    String description,
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @DecimalMax(value = "9999.99", message = "Price must not exceed 9999.99")
    BigDecimal price,
    
    @NotNull(message = "Category ID is required")
    @Positive(message = "Category ID must be positive")
    Long categoryId,
    
    @Min(value = 0, message = "Initial quantity must not be negative")
    @Max(value = 999999, message = "Initial quantity must not exceed 999999")
    int initialQuantity
) {}