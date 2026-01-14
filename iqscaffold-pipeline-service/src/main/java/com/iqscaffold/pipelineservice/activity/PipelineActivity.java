package com.iqscaffold.pipelineservice.activity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "pipeline_activities")
public class PipelineActivity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Column(name = "lead_id", nullable = false)
  private Long leadId;

  @Enumerated(EnumType.STRING)
  @Column(name = "activity_type", nullable = false)
  private ActivityType activityType;

  @NotBlank
  @Size(max = 500)
  @Column(name = "description", nullable = false, length = 500)
  private String description;

  @Size(max = 1000)
  @Column(name = "notes", length = 1000)
  private String notes;

  @Column(name = "old_stage_id")
  private Long oldStageId;

  @Column(name = "new_stage_id")
  private Long newStageId;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "created_by", nullable = false, updatable = false)
  private String createdBy;

  public PipelineActivity() {
  }

  public PipelineActivity(final Long leadId, final ActivityType activityType, final String description) {
    this.leadId = leadId;
    this.activityType = activityType;
    this.description = description;
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getLeadId() {
    return leadId;
  }

  public void setLeadId(Long leadId) {
    this.leadId = leadId;
  }

  public ActivityType getActivityType() {
    return activityType;
  }

  public void setActivityType(ActivityType activityType) {
    this.activityType = activityType;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public Long getOldStageId() {
    return oldStageId;
  }

  public void setOldStageId(Long oldStageId) {
    this.oldStageId = oldStageId;
  }

  public Long getNewStageId() {
    return newStageId;
  }

  public void setNewStageId(Long newStageId) {
    this.newStageId = newStageId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PipelineActivity that = (PipelineActivity) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "PipelineActivity{" +
           "id=" + id +
           ", leadId=" + leadId +
           ", activityType=" + activityType +
           ", description='" + description + '\'' +
           ", createdAt=" + createdAt +
           '}';
  }
}
