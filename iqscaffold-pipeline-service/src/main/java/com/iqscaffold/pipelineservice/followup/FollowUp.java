package com.iqscaffold.pipelineservice.followup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Objects;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "follow_ups")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@NamedEntityGraphs({
    @NamedEntityGraph(
        name = "FollowUp.basic"
        // No attributeNodes - just the basic entity (no relationships to load)
        )
})
public class FollowUp {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Column(name = "lead_id", nullable = false)
  private Long leadId;

  @NotBlank
  @Size(max = 200)
  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Size(max = 1000)
  @Column(name = "description", length = 1000)
  private String description;

  @NotNull
  @Column(name = "due_date", nullable = false)
  private LocalDateTime dueDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "priority", nullable = false)
  private FollowUpPriority priority = FollowUpPriority.MEDIUM;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private FollowUpStatus status = FollowUpStatus.PENDING;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Size(max = 100)
  @Column(name = "assigned_to", length = 100)
  private String assignedTo;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "created_by", nullable = false, updatable = false)
  private String createdBy;

  @Column(name = "updated_by", nullable = false)
  private String updatedBy;

  public FollowUp() {
  }

  public FollowUp(final Long leadId, final String title, final LocalDateTime dueDate) {
    this.leadId = leadId;
    this.title = title;
    this.dueDate = dueDate;
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

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public LocalDateTime getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDateTime dueDate) {
    this.dueDate = dueDate;
  }

  public FollowUpPriority getPriority() {
    return priority;
  }

  public void setPriority(FollowUpPriority priority) {
    this.priority = priority;
  }

  public FollowUpStatus getStatus() {
    return status;
  }

  public void setStatus(FollowUpStatus status) {
    this.status = status;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
  }

  public String getAssignedTo() {
    return assignedTo;
  }

  public void setAssignedTo(String assignedTo) {
    this.assignedTo = assignedTo;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public boolean isOverdue() {
    return status == FollowUpStatus.PENDING && dueDate.isBefore(LocalDateTime.now());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FollowUp followUp = (FollowUp) o;
    return Objects.equals(id, followUp.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "FollowUp{" +
           "id=" + id +
           ", leadId=" + leadId +
           ", title='" + title + '\'' +
           ", dueDate=" + dueDate +
           ", status=" + status +
           '}';
  }
}
