package com.iqscaffold.billingservice.shared;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA converter for Map to JSON string.
 */
@Converter
public class JsonMapConverter implements AttributeConverter<Map<String, Object>, String> {

  private static final Logger log = LoggerFactory.getLogger(JsonMapConverter.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> TYPE_REF = new TypeReference<>() {
  };

  @Override
  public String convertToDatabaseColumn(final Map<String, Object> attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(attribute);
    } catch (final JsonProcessingException e) {
      log.error("Error converting Map to JSON string", e);
      throw new IllegalArgumentException("Error converting Map to JSON", e);
    }
  }

  @Override
  public Map<String, Object> convertToEntityAttribute(final String dbData) {
    if (dbData == null || dbData.trim().isEmpty()) {
      return null;
    }
    try {
      return objectMapper.readValue(dbData, TYPE_REF);
    } catch (final JsonProcessingException e) {
      log.error("Error converting JSON string to Map: {}", dbData, e);
      throw new IllegalArgumentException("Error converting JSON to Map", e);
    }
  }
}
