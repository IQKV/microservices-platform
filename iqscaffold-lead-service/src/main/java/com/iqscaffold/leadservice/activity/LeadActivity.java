package com.iqscaffold.leadservice.activity;

import com.iqscaffold.leadservice.lead.Lead;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity representing an activity log entry for a lead.
 */
@Entity
@Table(name = "lead_activities")
public class LeadActivity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "lead_id", nullable = false)
  @NotNull
  private Lead lead;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 50)
  @NotNull
  private ActivityType type;

  @NotBlank
  @Column(name = "description", nullable = false, columnDefinition = "TEXT")
  private String description;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private String metadata;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "created_by", nullable = false, updatable = false)
  private String createdBy;

  // Constructors
  public LeadActivity() {}

  public LeadActivity(
      final Lead lead,
      final ActivityType type,
      final String description,
      final String createdBy) {
    this.lead = lead;
    this.type = type;
    this.description = description;
    this.createdBy = createdBy;
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(final Long id) {
    this.id = id;
  }

  public Lead getLead() {
    return lead;
  }

  public void setLead(final Lead lead) {
    this.lead = lead;
  }

  public ActivityType getType() {
    return type;
  }

  public void setType(final ActivityType type) {
    this.type = type;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getMetadata() {
    return metadata;
  }

  public void setMetadata(final String metadata) {
    this.metadata = metadata;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(final LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(final String createdBy) {
    this.createdBy = createdBy;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LeadActivity that = (LeadActivity) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "LeadActivity{"
        + "id=" + id
        + ", type=" + type
        + ", description='" + description + '\''
        + ", createdAt=" + createdAt
        + ", createdBy='" + createdBy + '\''
        + '}';
  }
}
