package com.iqscaffold.contactservice.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Entity representing a webhook subscription for external integrations.
 * <p>
 * Webhooks allow external systems to receive real-time notifications
 * when contact events occur (created, updated, deleted).
 */
@Entity
@Table(name = "webhooks")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Webhook {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "url", nullable = false, length = 500)
  private String url;

  @Column(name = "secret", length = 255)
  private String secret;

  @Column(name = "events", nullable = false, length = 500)
  private String events; // Comma-separated list of events

  @Column(name = "active", nullable = false)
  private Boolean active = true;

  @Column(name = "retry_count", nullable = false)
  private Integer retryCount = 3;

  @Column(name = "timeout_seconds", nullable = false)
  private Integer timeoutSeconds = 30;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "created_by", length = 100)
  private String createdBy;

  @Column(name = "updated_by", length = 100)
  private String updatedBy;

  @Column(name = "last_triggered_at")
  private LocalDateTime lastTriggeredAt;

  @Column(name = "success_count", nullable = false)
  private Long successCount = 0L;

  @Column(name = "failure_count", nullable = false)
  private Long failureCount = 0L;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // Getters and Setters

  public Long getId() {
    return id;
  }

  public void setId(final Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(final String url) {
    this.url = url;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(final String secret) {
    this.secret = secret;
  }

  public String getEvents() {
    return events;
  }

  public void setEvents(final String events) {
    this.events = events;
  }

  public Set<String> getEventSet() {
    Set<String> eventSet = new HashSet<>();
    if (events != null && !events.isEmpty()) {
      for (final String event : events.split(",")) {
        eventSet.add(event.trim());
      }
    }
    return eventSet;
  }

  public void setEventSet(final Set<String> eventSet) {
    this.events = String.join(",", eventSet);
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(final Boolean active) {
    this.active = active;
  }

  public Integer getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(final Integer retryCount) {
    this.retryCount = retryCount;
  }

  public Integer getTimeoutSeconds() {
    return timeoutSeconds;
  }

  public void setTimeoutSeconds(final Integer timeoutSeconds) {
    this.timeoutSeconds = timeoutSeconds;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(final LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(final LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(final String createdBy) {
    this.createdBy = createdBy;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(final String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public LocalDateTime getLastTriggeredAt() {
    return lastTriggeredAt;
  }

  public void setLastTriggeredAt(final LocalDateTime lastTriggeredAt) {
    this.lastTriggeredAt = lastTriggeredAt;
  }

  public Long getSuccessCount() {
    return successCount;
  }

  public void setSuccessCount(final Long successCount) {
    this.successCount = successCount;
  }

  public Long getFailureCount() {
    return failureCount;
  }

  public void setFailureCount(final Long failureCount) {
    this.failureCount = failureCount;
  }

  public void incrementSuccessCount() {
    this.successCount++;
    this.lastTriggeredAt = LocalDateTime.now();
  }

  public void incrementFailureCount() {
    this.failureCount++;
    this.lastTriggeredAt = LocalDateTime.now();
  }
}
