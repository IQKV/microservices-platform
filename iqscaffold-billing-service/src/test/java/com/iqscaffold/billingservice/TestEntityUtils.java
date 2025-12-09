package com.iqscaffold.billingservice;

import java.lang.reflect.Field;

/**
 * Utility class for test entity manipulation.
 * 
 * <p>Provides helper methods to set private fields on entities for testing purposes.
 * This is necessary because JPA entities don't have public setters for ID fields.
 */
public class TestEntityUtils {

  /**
   * Sets the ID field on an entity using reflection.
   * 
   * @param entity the entity to modify
   * @param id the ID value to set
   * @param <T> the entity type
   * @throws RuntimeException if reflection fails
   */
  public static <T> void setId(T entity, Long id) {
    try {
      Field idField = entity.getClass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(entity, id);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("Failed to set ID on entity", e);
    }
  }

  /**
   * Sets a private field on an entity using reflection.
   * 
   * @param entity the entity to modify
   * @param fieldName the name of the field to set
   * @param value the value to set
   * @param <T> the entity type
   * @throws RuntimeException if reflection fails
   */
  public static <T> void setField(T entity, String fieldName, Object value) {
    try {
      Field field = entity.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(entity, value);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("Failed to set field " + fieldName + " on entity", e);
    }
  }

  private TestEntityUtils() {
    // Utility class
  }
}
