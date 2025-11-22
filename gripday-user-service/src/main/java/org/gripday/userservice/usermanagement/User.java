package org.gripday.userservice.usermanagement;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.gripday.userservice.organization.Organization;
import org.gripday.userservice.shared.Authority;
import org.gripday.userservice.shared.TenantAware;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * User entity representing user accounts with authentication and profile information. Supports multi-tenant architecture with tenant isolation.
 */
@Entity
@Table(name = "users")
public class User extends TenantAware {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "username", nullable = false, unique = true, length = 50)
  private String username;

  @Column(name = "email", nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Column(name = "enabled", nullable = false)
  private Boolean enabled = true;

  @Column(name = "email_verified", nullable = false)
  private Boolean emailVerified = false;

  @Column(name = "preferred_locale", length = 10)
  private String preferredLocale = "en";

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;


  @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "user_authorities",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "authority_id")
  )
  private Set<Authority> authorities = new HashSet<>();

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id")
  private Organization organization;

  @OneToOne(mappedBy = "user")
  private UserPreference preference;

  // Default constructor for JPA
  protected User() {
  }

  // Constructor with required fields
  public User(final String username, final String email, final String passwordHash,
              final String firstName, final String lastName, final String tenantId) {
    super(tenantId);
    this.username = username;
    this.email = email;
    this.passwordHash = passwordHash;
    this.firstName = firstName;
    this.lastName = lastName;
  }

  // Getters and setters using modern Java syntax
  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
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

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Boolean getEmailVerified() {
    return emailVerified;
  }

  public void setEmailVerified(Boolean emailVerified) {
    this.emailVerified = emailVerified;
  }

  public String getPreferredLocale() {
    return preferredLocale;
  }

  public void setPreferredLocale(String preferredLocale) {
    this.preferredLocale = preferredLocale;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }


  public Set<Authority> getAuthorities() {
    return authorities;
  }

  public void setAuthorities(Set<Authority> authorities) {
    this.authorities = authorities;
  }

  public Organization getOrganization() {
    return organization;
  }

  public void setOrganization(Organization organization) {
    this.organization = organization;
  }

  public UserPreference getPreference() {
    return preference;
  }

  public void setPreference(UserPreference preference) {
    this.preference = preference;
  }

  // Utility methods
  public void addAuthority(Authority authority) {
    var currentAuthorities = this.authorities;
    currentAuthorities.add(authority);
    authority.getUsers().add(this);
  }

  public void removeAuthority(Authority authority) {
    var currentAuthorities = this.authorities;
    currentAuthorities.remove(authority);
    authority.getUsers().remove(this);
  }

  public boolean hasAuthority(String authorityName) {
    var authorities = this.authorities;
    return authorities.stream()
        .anyMatch(authority -> authority.getName().equals(authorityName));
  }

  public String getFullName() {
    var first = this.firstName;
    var last = this.lastName;
    return first + " " + last;
  }

  public boolean isActive() {
    var enabled = this.enabled;
    var emailVerified = this.emailVerified;
    return enabled != null && enabled && emailVerified != null && emailVerified;
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

    var user = (User) obj;
    return Objects.equals(id, user.id)
           && Objects.equals(username, user.username)
           && Objects.equals(email, user.email);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, username, email);
  }

  @Override
  public String toString() {
    var sb = new StringBuilder();
    sb.append("User{")
        .append("id=").append(id)
        .append(", username='").append(username).append('\'')
        .append(", email='").append(email).append('\'')
        .append(", firstName='").append(firstName).append('\'')
        .append(", lastName='").append(lastName).append('\'')
        .append(", enabled=").append(enabled)
        .append(", emailVerified=").append(emailVerified)
        .append(", tenantId='").append(getTenantId()).append('\'')
        .append(", createdAt=").append(createdAt)
        .append('}');
    return sb.toString();
  }
}
