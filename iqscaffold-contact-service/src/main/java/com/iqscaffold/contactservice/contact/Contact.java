package com.iqscaffold.contactservice.contact;

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
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Objects;

import com.iqscaffold.contactservice.company.Company;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "contacts")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@NamedEntityGraphs({
  @NamedEntityGraph(
    name = "contact-with-company",
    attributeNodes = {
      @jakarta.persistence.NamedAttributeNode("company")
    }
  )
})
public class Contact {

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
  @Size(max = 255)
  @Column(name = "email", length = 255, unique = true)
  private String email;

  @Size(max = 20)
  @Column(name = "phone", length = 20)
  private String phone;

  @Size(max = 100)
  @Column(name = "job_title", length = 100)
  private String jobTitle;

  @Column(name = "company_id")
  private Long companyId;

  /**
   * JPA relationship to Company entity.
   * This provides an alternative to using companyId for queries that need company details.
   * Both companyId and company relationship are maintained for backward compatibility.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", insertable = false, updatable = false)
  private Company company;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ContactStatus status = ContactStatus.ACTIVE;

  @Column(name = "lead_score")
  private Integer leadScore = 0;

  @Size(max = 1000)
  @Column(name = "notes", length = 1000)
  private String notes;

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

  @Column(name = "converted_from_lead_id")
  private Long convertedFromLeadId;

  @Column(name = "converted_at")
  private LocalDateTime convertedAt;

  // Constructors
  public Contact() {
  }

  public Contact(final String firstName, final String lastName, final String email) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getJobTitle() {
    return jobTitle;
  }

  public void setJobTitle(String jobTitle) {
    this.jobTitle = jobTitle;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public void setCompanyId(Long companyId) {
    this.companyId = companyId;
  }

  public Company getCompany() {
    return company;
  }

  public void setCompany(Company company) {
    this.company = company;
  }

  public ContactStatus getStatus() {
    return status;
  }

  public void setStatus(ContactStatus status) {
    this.status = status;
  }

  public Integer getLeadScore() {
    return leadScore;
  }

  public void setLeadScore(Integer leadScore) {
    this.leadScore = leadScore;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
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

  public Long getConvertedFromLeadId() {
    return convertedFromLeadId;
  }

  public void setConvertedFromLeadId(Long convertedFromLeadId) {
    this.convertedFromLeadId = convertedFromLeadId;
  }

  public LocalDateTime getConvertedAt() {
    return convertedAt;
  }

  public void setConvertedAt(LocalDateTime convertedAt) {
    this.convertedAt = convertedAt;
  }

  // Helper methods
  public String getFullName() {
    return firstName + " " + lastName;
  }

  public boolean isConvertedFromLead() {
    return convertedFromLeadId != null;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Contact contact = (Contact) o;
    return Objects.equals(id, contact.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "Contact{" +
           "id=" + id +
           ", firstName='" + firstName + '\'' +
           ", lastName='" + lastName + '\'' +
           ", email='" + email + '\'' +
           ", status=" + status +
           '}';
  }
}