package com.iqscaffold.pipelineservice.pipeline;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "pipeline_items")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@NamedEntityGraphs({
    @NamedEntityGraph(
        name = "PipelineItem.withStage",
        attributeNodes = {
            @jakarta.persistence.NamedAttributeNode("stage")
        }
    ),
    @NamedEntityGraph(
        name = "PipelineItem.basic"
        // No attributeNodes - just the basic entity
        )
})
public class PipelineItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Column(name = "lead_id", nullable = false)
  private Long leadId;

  @NotNull
  @Column(name = "stage_id", nullable = false)
  private Long stageId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "stage_id", insertable = false, updatable = false)
  private PipelineStage stage;

  @Column(name = "expected_value", precision = 15, scale = 2)
  private BigDecimal expectedValue;

  @Column(name = "probability", precision = 5, scale = 2)
  private BigDecimal probability;

  @Column(name = "entered_stage_at", nullable = false)
  private LocalDateTime enteredStageAt;

  @Column(name = "days_in_stage")
  private Integer daysInStage;

  @Column(name = "converted_at")
  private LocalDateTime convertedAt;

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

  public PipelineItem() {
  }

  public PipelineItem(final Long leadId, final Long stageId) {
    this.leadId = leadId;
    this.stageId = stageId;
    this.enteredStageAt = LocalDateTime.now();
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

  public Long getStageId() {
    return stageId;
  }

  public void setStageId(Long stageId) {
    this.stageId = stageId;
  }

  public PipelineStage getStage() {
    return stage;
  }

  public void setStage(PipelineStage stage) {
    this.stage = stage;
  }

  public BigDecimal getExpectedValue() {
    return expectedValue;
  }

  public void setExpectedValue(BigDecimal expectedValue) {
    this.expectedValue = expectedValue;
  }

  public BigDecimal getProbability() {
    return probability;
  }

  public void setProbability(BigDecimal probability) {
    this.probability = probability;
  }

  public LocalDateTime getEnteredStageAt() {
    return enteredStageAt;
  }

  public void setEnteredStageAt(LocalDateTime enteredStageAt) {
    this.enteredStageAt = enteredStageAt;
  }

  public Integer getDaysInStage() {
    return daysInStage;
  }

  public void setDaysInStage(Integer daysInStage) {
    this.daysInStage = daysInStage;
  }

  public LocalDateTime getConvertedAt() {
    return convertedAt;
  }

  public void setConvertedAt(LocalDateTime convertedAt) {
    this.convertedAt = convertedAt;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PipelineItem that = (PipelineItem) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "PipelineItem{" +
           "id=" + id +
           ", leadId=" + leadId +
           ", stageId=" + stageId +
           ", enteredStageAt=" + enteredStageAt +
           '}';
  }
}
