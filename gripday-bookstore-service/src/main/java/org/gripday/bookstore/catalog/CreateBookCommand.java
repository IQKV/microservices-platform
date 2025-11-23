package org.gripday.bookstore.catalog;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Command to create a new book in the catalog")
public record CreateBookCommand(
    @Schema(description = "Title of the book", example = "The Great Gatsby", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    String title,

    @Schema(description = "Author of the book", example = "F. Scott Fitzgerald", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Author is required")
    @Size(max = 255, message = "Author must not exceed 255 characters")
    String author,

    @Schema(description = "ISBN-13 of the book", example = "978-0-7432-7356-5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "ISBN is required")
    @Pattern(
        regexp = "^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$",
        message = "Invalid ISBN format")
    String isbn,

    @Schema(description = "Detailed description of the book", example = "A classic American novel set in the Jazz Age")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    String description,

    @Schema(description = "Price of the book in USD", example = "12.99", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer digits and 2 decimal places")
    BigDecimal price,

    @Schema(description = "ID of the category this book belongs to", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Category ID is required")
    Long categoryId,

    @Schema(description = "Initial inventory quantity for the book", example = "25")
    @Min(value = 0, message = "Initial quantity cannot be negative")
    int initialQuantity
) {

}
