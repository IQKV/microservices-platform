package org.gripday.userservice.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

import org.gripday.userservice.shared.TenantAware;
import org.gripday.userservice.usermanagement.User;
import org.hibernate.annotations.CreationTimestamp;

/**
 * UserAuditLog entity for tracking security-related user actions and events. Provides audit trail for compliance and security monitoring.
 */
@Entity
@Table(name = "user_audit_log")
public class UserAuditLog extends TenantAware {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id")
  private Long userId;

  @Column(name = "action", nullable = false, length = 100)
  private String action;

  @Column(name = "details", columnDefinition = "TEXT")
  private String details;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @Column(name = "user_agent", columnDefinition = "TEXT")
  private String userAgent;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;


  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  private User user;

  // Default constructor for JPA
  protected UserAuditLog() {
  }

  // Constructor with required fields
  public UserAuditLog(final String action, final String tenantId) {
    super(tenantId);
    this.action = action;
  }

  // Constructor with user context
  public UserAuditLog(final Long userId, final String action, final String tenantId) {
    super(tenantId);
    this.userId = userId;
    this.action = action;
  }

  // Full constructor
  public UserAuditLog(final Long userId, final String action, final String details,
      final String ipAddress, final String userAgent, final String tenantId) {
    super(tenantId);
    this.userId = userId;
    this.action = action;
    this.details = details;
    this.ipAddress = ipAddress;
    this.userAgent = userAgent;
  }

  // Getters and setters
  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }


  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  // Utility methods
  public boolean isLoginAction() {
    var action = this.action;
    return "LOGIN_SUCCESS".equals(action) || "LOGIN_FAILURE".equals(action);
  }

  public boolean isSecurityAction() {
    var action = this.action;
    return action != null && (
        action.startsWith("LOGIN_")
            || action.startsWith("LOGOUT_")
            || action.startsWith("PASSWORD_")
            || action.startsWith("ACCOUNT_"));
  }

  public String getActionCategory() {
    var action = this.action;
    if (action == null) {
      return "UNKNOWN";
    }

    return switch (action) {
      case String a when a.startsWith("LOGIN_") -> "AUTHENTICATION";
      case String a when a.startsWith("LOGOUT_") -> "AUTHENTICATION";
      case String a when a.startsWith("PASSWORD_") -> "SECURITY";
      case String a when a.startsWith("ACCOUNT_") -> "ACCOUNT_MANAGEMENT";
      case String a when a.startsWith("ROLE_") -> "AUTHORIZATION";
      default -> "GENERAL";
    };
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

    var auditLog = (UserAuditLog) obj;
    return Objects.equals(id, auditLog.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    var sb = new StringBuilder();
    sb.append("UserAuditLog{")
        .append("id=").append(id)
        .append(", userId=").append(userId)
        .append(", action='").append(action).append('\'')
        .append(", ipAddress='").append(ipAddress).append('\'')
        .append(", tenantId='").append(getTenantId()).append('\'')
        .append(", createdAt=").append(createdAt)
        .append('}');
    return sb.toString();
  }
}
