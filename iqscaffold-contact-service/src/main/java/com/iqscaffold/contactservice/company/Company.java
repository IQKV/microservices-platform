package com.iqscaffold.contactservice.company;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.iqscaffold.contactservice.contact.Contact;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "companies")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@NamedEntityGraphs({
  @NamedEntityGraph(
    name = "company-with-contacts",
    attributeNodes = {
      @jakarta.persistence.NamedAttributeNode("contacts")
    }
  ),
  @NamedEntityGraph(
    name = "company-with-parent",
    attributeNodes = {
      @jakarta.persistence.NamedAttributeNode("parentCompany")
    }
  ),
  @NamedEntityGraph(
    name = "company-with-children",
    attributeNodes = {
      @jakarta.persistence.NamedAttributeNode("childCompanies")
    }
  ),
  @NamedEntityGraph(
    name = "company-with-hierarchy",
    attributeNodes = {
      @jakarta.persistence.NamedAttributeNode("parentCompany"),
      @jakarta.persistence.NamedAttributeNode("childCompanies")
    }
  ),
  @NamedEntityGraph(
    name = "company-complete",
    attributeNodes = {
      @jakarta.persistence.NamedAttributeNode("contacts"),
      @jakarta.persistence.NamedAttributeNode("parentCompany"),
      @jakarta.persistence.NamedAttributeNode("childCompanies")
    }
  )
})
public class Company {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Size(max = 255)
  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Size(max = 255)
  @Column(name = "website", length = 255)
  private String website;

  @Size(max = 100)
  @Column(name = "industry", length = 100)
  private String industry;

  @Size(max = 50)
  @Column(name = "size", length = 50)
  private String size;

  @Size(max = 20)
  @Column(name = "phone", length = 20)
  private String phone;

  @Email
  @Size(max = 255)
  @Column(name = "email", length = 255)
  private String email;

  @Size(max = 255)
  @Column(name = "address_line1", length = 255)
  private String addressLine1;

  @Size(max = 255)
  @Column(name = "address_line2", length = 255)
  private String addressLine2;

  @Size(max = 100)
  @Column(name = "city", length = 100)
  private String city;

  @Size(max = 100)
  @Column(name = "state", length = 100)
  private String state;

  @Size(max = 20)
  @Column(name = "postal_code", length = 20)
  private String postalCode;

  @Size(max = 100)
  @Column(name = "country", length = 100)
  private String country;

  @Column(name = "parent_company_id")
  private Long parentCompanyId;

  /**
   * JPA relationship to parent Company entity.
   * This provides an alternative to using parentCompanyId for queries that need parent company details.
   * Both parentCompanyId and parentCompany relationship are maintained for backward compatibility.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_company_id", insertable = false, updatable = false)
  private Company parentCompany;

  /**
   * JPA relationship to child Company entities.
   * This provides access to subsidiary companies.
   */
  @OneToMany(mappedBy = "parentCompany", fetch = FetchType.LAZY)
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private List<Company> childCompanies = new ArrayList<>();

  /**
   * JPA relationship to Contact entities.
   * This provides access to all contacts associated with this company.
   */
  @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  private List<Contact> contacts = new ArrayList<>();

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private CompanyStatus status = CompanyStatus.ACTIVE;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "created_by", nullable = false, updatable = false, length = 100)
  private String createdBy;

  @Column(name = "updated_by", nullable = false, length = 100)
  private String updatedBy;

  // Constructors
  public Company() {
  }

  public Company(final String name) {
    this.name = name;
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getWebsite() {
    return website;
  }

  public void setWebsite(String website) {
    this.website = website;
  }

  public String getIndustry() {
    return industry;
  }

  public void setIndustry(String industry) {
    this.industry = industry;
  }

  public String getSize() {
    return size;
  }

  public void setSize(String size) {
    this.size = size;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getAddressLine1() {
    return addressLine1;
  }

  public void setAddressLine1(String addressLine1) {
    this.addressLine1 = addressLine1;
  }

  public String getAddressLine2() {
    return addressLine2;
  }

  public void setAddressLine2(String addressLine2) {
    this.addressLine2 = addressLine2;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public Long getParentCompanyId() {
    return parentCompanyId;
  }

  public void setParentCompanyId(Long parentCompanyId) {
    this.parentCompanyId = parentCompanyId;
  }

  public Company getParentCompany() {
    return parentCompany;
  }

  public void setParentCompany(Company parentCompany) {
    this.parentCompany = parentCompany;
  }

  public List<Company> getChildCompanies() {
    return childCompanies;
  }

  public void setChildCompanies(List<Company> childCompanies) {
    this.childCompanies = childCompanies;
  }

  public List<Contact> getContacts() {
    return contacts;
  }

  public void setContacts(List<Contact> contacts) {
    this.contacts = contacts;
  }

  public CompanyStatus getStatus() {
    return status;
  }

  public void setStatus(CompanyStatus status) {
    this.status = status;
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

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Company company = (Company) o;
    return Objects.equals(id, company.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "Company{" +
           "id=" + id +
           ", name='" + name + '\'' +
           ", industry='" + industry + '\'' +
           ", status=" + status +
           '}';
  }
}