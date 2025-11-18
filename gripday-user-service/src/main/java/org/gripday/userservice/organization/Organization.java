package org.gripday.userservice.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

import org.gripday.userservice.shared.TenantAware;
import org.gripday.userservice.usermanagement.User;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Organization entity representing company or organizational units with OneToOne relationship to User.
 */
@Entity
@Table(name = "organizations")
public class Organization extends TenantAware {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "description", length = 1000)
  private String description;

  @Column(name = "industry", length = 100)
  private String industry;

  @Column(name = "website", length = 255)
  private String website;

  @Column(name = "phone", length = 50)
  private String phone;

  @Column(name = "address", length = 500)
  private String address;

  @Column(name = "city", length = 100)
  private String city;

  @Column(name = "country", length = 100)
  private String country;

  @Column(name = "enabled", nullable = false)
  private Boolean enabled = true;

  @OneToOne(mappedBy = "organization")
  private User owner;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  protected Organization() {
  }

  public Organization(final String name, final String tenantId) {
    super(tenantId);
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getIndustry() {
    return industry;
  }

  public void setIndustry(String industry) {
    this.industry = industry;
  }

  public String getWebsite() {
    return website;
  }

  public void setWebsite(String website) {
    this.website = website;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public User getOwner() {
    return owner;
  }

  public void setOwner(User owner) {
    this.owner = owner;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public boolean isActive() {
    var enabled = this.enabled;
    return enabled != null && enabled;
  }

  public String getLocation() {
    var city = this.city;
    var country = this.country;
    if (city != null && country != null) {
      return city + ", " + country;
    }
    return city != null ? city : (country != null ? country : "");
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    var organization = (Organization) obj;
    return Objects.equals(id, organization.id)
        && Objects.equals(name, organization.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }

  @Override
  public String toString() {
    var sb = new StringBuilder();
    sb.append("Organization{")
        .append("id=").append(id)
        .append(", name='").append(name).append('\'')
        .append(", industry='").append(industry).append('\'')
        .append(", enabled=").append(enabled)
        .append(", tenantId='").append(getTenantId()).append('\'')
        .append(", createdAt=").append(createdAt)
        .append('}');
    return sb.toString();
  }
}
