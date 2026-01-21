package com.iqscaffold.billingservice.shared;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA converter for List to JSON string.
 */
@Converter
public class JsonListConverter implements AttributeConverter<List<String>, String> {

  private static final Logger log = LoggerFactory.getLogger(JsonListConverter.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final TypeReference<List<String>> TYPE_REF = new TypeReference<>() {
  };

  @Override
  public String convertToDatabaseColumn(final List<String> attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (final JsonProcessingException e) {
      log.error("Error converting List to JSON string", e);
      throw new IllegalArgumentException("Error converting List to JSON", e);
    }
  }

  @Override
  public List<String> convertToEntityAttribute(final String dbData) {
    if (dbData == null || dbData.trim().isEmpty()) {
      return null;
    }
    try {
      return objectMapper.readValue(dbData, TYPE_REF);
    } catch (final JsonProcessingException e) {
      log.error("Error converting JSON string to List: {}", dbData, e);
      throw new IllegalArgumentException("Error converting JSON to List", e);
    }
  }
}
