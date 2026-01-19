package com.iqscaffold.billingservice.feature;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity for tracking feature usage across tenants.
 *
 * <p>Stores usage events for analytics, billing, and monitoring purposes.
 * This data helps understand feature adoption and usage patterns.
 *
 * <p>Stored in tenant-specific schema for data isolation.
 */
@Entity
@Table(
    name = "feature_usage_log",
    indexes = {
        @Index(name = "idx_feature_usage_tenant_feature", columnList = "tenant_id, feature_key"),
        @Index(name = "idx_feature_usage_timestamp", columnList = "timestamp"),
        @Index(name = "idx_feature_usage_tenant_timestamp", columnList = "tenant_id, timestamp"),
        @Index(name = "idx_feature_usage_feature_timestamp", columnList = "feature_key, timestamp")
    }
)
public class FeatureUsageLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false, length = 100)
  private String tenantId;

  @Column(name = "feature_key", nullable = false, length = 100)
  private String featureKey;

  @Column(name = "endpoint", length = 500)
  private String endpoint;

  @Column(name = "user_id", length = 100)
  private String userId;

  @Column(name = "timestamp", nullable = false)
  private Instant timestamp;

  @Column(name = "session_id", length = 100)
  private String sessionId;

  @Column(name = "correlation_id", length = 100)
  private String correlationId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private Map<String, Object> metadata;

  // Constructors
  public FeatureUsageLog() {
  }

  public FeatureUsageLog(final String tenantId, final String featureKey, final String endpoint) {
    this.tenantId = tenantId;
    this.featureKey = featureKey;
    this.endpoint = endpoint;
  }

  public FeatureUsageLog(final String tenantId, final String featureKey, final String endpoint, final String userId) {
    this.tenantId = tenantId;
    this.featureKey = featureKey;
    this.endpoint = endpoint;
    this.userId = userId;
  }

  // Lifecycle callbacks
  @PrePersist
  protected void onCreate() {
    if (timestamp == null) {
      timestamp = Instant.now();
    }
  }

  // Getters and setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getFeatureKey() {
    return featureKey;
  }

  public void setFeatureKey(String featureKey) {
    this.featureKey = featureKey;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FeatureUsageLog that = (FeatureUsageLog) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "FeatureUsageLog{" +
           "id=" + id +
           ", tenantId='" + tenantId + '\'' +
           ", featureKey='" + featureKey + '\'' +
           ", endpoint='" + endpoint + '\'' +
           ", timestamp=" + timestamp +
           '}';
  }
}
