package org.gripday.authservice.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;

/**
 * Entity representing email verification tokens for user account activation. Tokens are time-limited, single-use, and tenant-aware for security.
 */
@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken extends TenantAwareEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "token", nullable = false, unique = true, length = 255)
  private String token;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "used", nullable = false)
  private Boolean used = false;

  // Default constructor for JPA
  protected EmailVerificationToken() {
  }

  // Constructor with required fields using Java 21 features
  public EmailVerificationToken(String token, Long userId, LocalDateTime expiresAt, String tenantId) {
    super(tenantId);
    this.token = token;
    this.userId = userId;
    this.expiresAt = expiresAt;
  }

  // Getters and setters using modern Java syntax
  public Long getId() {
    return id;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(LocalDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public Boolean getUsed() {
    return used;
  }

  public void setUsed(Boolean used) {
    this.used = used;
  }

  // Utility methods using Java 21 features
  public boolean isExpired() {
    var now = LocalDateTime.now();
    var expirationTime = this.expiresAt;
    return expirationTime != null && now.isAfter(expirationTime);
  }

  public boolean isValid() {
    var isUsed = this.used;
    var isExpired = isExpired();
    return isUsed != null && !isUsed && !isExpired;
  }

  public void markAsUsed() {
    this.used = true;
  }

  public boolean isUnused() {
    var isUsed = this.used;
    return isUsed == null || !isUsed;
  }

  // Standard equals, hashCode, and toString methods
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    var that = (EmailVerificationToken) obj;
    return Objects.equals(id, that.id)
        && Objects.equals(token, that.token)
        && Objects.equals(userId, that.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, token, userId);
  }

  @Override
  public String toString() {
    var sb = new StringBuilder();
    sb.append("EmailVerificationToken{")
        .append("id=").append(id)
        .append(", token='").append(token != null ? token.substring(0, Math.min(8, token.length())) + "..." : null).append('\'')
        .append(", userId=").append(userId)
        .append(", expiresAt=").append(expiresAt)
        .append(", createdAt=").append(createdAt)
        .append(", used=").append(used)
        .append(", tenantId='").append(getTenantId()).append('\'')
        .append('}');
    return sb.toString();
  }
}