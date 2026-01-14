package com.iqscaffold.leadservice.note;

import com.iqscaffold.leadservice.lead.Lead;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Entity representing a note attached to a lead.
 * Notes allow sales representatives to document conversations and important information.
 */
@Entity
@Table(name = "lead_notes")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class LeadNote {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "lead_id", nullable = false)
  private Lead lead;

  @NotBlank
  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "is_pinned", nullable = false)
  private Boolean isPinned = false;

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

  // Constructors
  public LeadNote() {}

  public LeadNote(final Lead lead, final String content, final String createdBy) {
    this.lead = lead;
    this.content = content;
    this.createdBy = createdBy;
    this.updatedBy = createdBy;
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

  public String getContent() {
    return content;
  }

  public void setContent(final String content) {
    this.content = content;
  }

  public Boolean getIsPinned() {
    return isPinned;
  }

  public void setIsPinned(final Boolean isPinned) {
    this.isPinned = isPinned;
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

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LeadNote leadNote = (LeadNote) o;
    return Objects.equals(id, leadNote.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "LeadNote{"
        + "id=" + id
        + ", leadId=" + (lead != null ? lead.getId() : null)
        + ", content='" + (content != null && content.length() > 50 
            ? content.substring(0, 50) + "..." 
            : content) + '\''
        + ", isPinned=" + isPinned
        + ", createdAt=" + createdAt
        + '}';
  }
}
