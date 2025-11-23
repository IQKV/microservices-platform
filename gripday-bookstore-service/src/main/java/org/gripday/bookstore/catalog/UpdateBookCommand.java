package org.gripday.bookstore.catalog;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Command to update an existing book")
public record UpdateBookCommand(
    @Schema(description = "Title of the book", example = "The Great Gatsby", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    String title,

    @Schema(description = "Author of the book", example = "F. Scott Fitzgerald", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Author is required")
    @Size(max = 255, message = "Author must not exceed 255 characters")
    String author,

    @Schema(description = "Detailed description of the book", example = "A classic American novel")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    String description,

    @Schema(description = "Price of the book in USD", example = "12.99", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer digits and 2 decimal places")
    BigDecimal price,

    @Schema(description = "ID of the category", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Category ID is required")
    Long categoryId
) {

}
