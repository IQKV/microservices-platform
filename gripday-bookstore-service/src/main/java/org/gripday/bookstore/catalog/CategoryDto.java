package org.gripday.bookstore.catalog;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Category information")
public record CategoryDto(
    @Schema(description = "Unique identifier of the category", example = "1")
    Long id,

    @Schema(description = "Name of the category", example = "Fiction")
    String name,

    @Schema(description = "Description of the category", example = "Fiction books and novels")
    String description,

    @Schema(description = "Number of books in this category", example = "42")
    int bookCount,

    @Schema(description = "Timestamp when the category was created")
    LocalDateTime createdAt,

    @Schema(description = "Timestamp when the category was last updated")
    LocalDateTime updatedAt
) {

  public static CategoryDto from(Category category) {
    return new CategoryDto(
        category.getId(),
        category.getName(),
        category.getDescription(),
        category.getBooks() != null ? category.getBooks().size() : 0,
        category.getCreatedAt(),
        category.getUpdatedAt()
    );
  }
}
