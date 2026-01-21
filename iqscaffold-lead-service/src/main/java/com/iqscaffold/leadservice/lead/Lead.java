package com.iqscaffold.leadservice.lead;

import com.iqscaffold.leadservice.activity.LeadActivity;
import com.iqscaffold.leadservice.note.LeadNote;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "leads")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@NamedEntityGraph(
    name = "Lead.withNotes",
    attributeNodes = @NamedAttributeNode("notes")
)
@NamedEntityGraph(
    name = "Lead.withActivities", 
    attributeNodes = @NamedAttributeNode("activities")
)
@NamedEntityGraph(
    name = "Lead.withNotesAndActivities",
    attributeNodes = {
        @NamedAttributeNode("notes"),
        @NamedAttributeNode("activities")
    }
)
@NamedEntityGraph(
    name = "Lead.basic"
    // No attributeNodes - just the basic Lead entity without collections
)
public class Lead {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Size(max = 100)
  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @NotBlank
  @Size(max = 100)
  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Email
  @NotBlank
  @Size(max = 255)
  @Column(name = "email", nullable = false, length = 255, unique = true)
  private String email;

  @Size(max = 20)
  @Column(name = "phone", length = 20)
  private String phone;

  @Size(max = 255)
  @Column(name = "company", length = 255)
  private String company;

  @Size(max = 100)
  @Column(name = "job_title", length = 100)
  private String jobTitle;

  @NotBlank
  @Size(max = 100)
  @Column(name = "source", nullable = false, length = 100)
  private String source;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private LeadStatus status = LeadStatus.NEW;

  @Column(name = "score")
  private Integer score = 0;

  @Column(name = "qualified")
  private Boolean qualified = false;

  @Size(max = 2000)
  @Column(name = "notes", length = 2000)
  private String notes;

  @Column(name = "converted_at")
  private LocalDateTime convertedAt;

  @Column(name = "converted_to_contact_id")
  private Long convertedToContactId;

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

  @Column(name = "assigned_to")
  private String assignedTo;

  // Relationships
  @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  @OrderBy("createdAt DESC")
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private List<LeadNote> notes = new ArrayList<>();

  @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  @OrderBy("createdAt DESC")
  private List<LeadActivity> activities = new ArrayList<>();

  // Constructors
  public Lead() {
  }

  public Lead(final String firstName, final String lastName, final String email, final String source) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.source = source;
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(final Long id) {
    this.id = id;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(final String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(final String lastName) {
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(final String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(final String phone) {
    this.phone = phone;
  }

  public String getCompany() {
    return company;
  }

  public void setCompany(final String company) {
    this.company = company;
  }

  public String getJobTitle() {
    return jobTitle;
  }

  public void setJobTitle(final String jobTitle) {
    this.jobTitle = jobTitle;
  }

  public String getSource() {
    return source;
  }

  public void setSource(final String source) {
    this.source = source;
  }

  public LeadStatus getStatus() {
    return status;
  }

  public void setStatus(final LeadStatus status) {
    this.status = status;
  }

  public Integer getScore() {
    return score;
  }

  public void setScore(final Integer score) {
    this.score = score;
  }

  public Boolean getQualified() {
    return qualified;
  }

  public void setQualified(final Boolean qualified) {
    this.qualified = qualified;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(final String notes) {
    this.notes = notes;
  }

  public LocalDateTime getConvertedAt() {
    return convertedAt;
  }

  public void setConvertedAt(final LocalDateTime convertedAt) {
    this.convertedAt = convertedAt;
  }

  public Long getConvertedToContactId() {
    return convertedToContactId;
  }

  public void setConvertedToContactId(final Long convertedToContactId) {
    this.convertedToContactId = convertedToContactId;
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

  public String getAssignedTo() {
    return assignedTo;
  }

  public void setAssignedTo(final String assignedTo) {
    this.assignedTo = assignedTo;
  }

  public List<LeadNote> getNotes() {
    return notes;
  }

  public void setNotes(final List<LeadNote> notes) {
    this.notes = notes;
  }

  public List<LeadActivity> getActivities() {
    return activities;
  }

  public void setActivities(final List<LeadActivity> activities) {
    this.activities = activities;
  }

  // Helper methods for managing relationships
  public void addNote(final LeadNote note) {
    notes.add(note);
    note.setLead(this);
  }

  public void removeNote(final LeadNote note) {
    notes.remove(note);
    note.setLead(null);
  }

  public void addActivity(final LeadActivity activity) {
    activities.add(activity);
    activity.setLead(this);
  }

  public void removeActivity(final LeadActivity activity) {
    activities.remove(activity);
    activity.setLead(null);
  }

  // Helper methods
  public String getFullName() {
    return firstName + " " + lastName;
  }

  public boolean isConverted() {
    return status == LeadStatus.CONVERTED && convertedAt != null;
  }

  public boolean isQualified() {
    return qualified != null && qualified;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Lead lead = (Lead) o;
    return Objects.equals(id, lead.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "Lead{"
           + "id=" + id
           + ", firstName='" + firstName + '\''
           + ", lastName='" + lastName + '\''
           + ", email='" + email + '\''
           + ", company='" + company + '\''
           + ", source='" + source + '\''
           + ", status=" + status
           + ", score=" + score
           + '}';
  }
}
