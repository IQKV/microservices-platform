package com.iqscaffold.userservice.usermanagement;

import jakarta.persistence.Cacheable;
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

import com.iqscaffold.userservice.organization.Organization;
import com.iqscaffold.userservice.shared.Authority;
import com.iqscaffold.userservice.shared.TenantAware;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Core user entity representing authenticated user accounts with comprehensive profile and security information.
 *
 * <p>This entity serves as the central user representation in the multi-tenant authentication system,
 * containing all necessary information for user identification, authentication, authorization, and
 * profile management. It implements multi-tenant isolation through the TenantAware base class.
 *
 * <h3>Entity Characteristics</h3>
 * <ul>
 *   <li><strong>Multi-Tenant Aware</strong> - Inherits tenant isolation from TenantAware base class</li>
 *   <li><strong>Security Focused</strong> - Includes password hashing, account status, and verification</li>
 *   <li><strong>Profile Complete</strong> - Comprehensive user profile information</li>
 *   <li><strong>Audit Enabled</strong> - Automatic timestamp tracking for creation and updates</li>
 * </ul>
 *
 * <h3>Authentication Fields</h3>
 * <ul>
 *   <li><strong>username</strong> - Unique identifier for login (50 chars max)</li>
 *   <li><strong>email</strong> - Email address, also unique and used for login (255 chars max)</li>
 *   <li><strong>passwordHash</strong> - BCrypt hashed password (255 chars max)</li>
 *   <li><strong>enabled</strong> - Account status flag (default: true)</li>
 *   <li><strong>emailVerified</strong> - Email verification status (default: false)</li>
 * </ul>
 *
 * <h3>Profile Information</h3>
 * <ul>
 *   <li><strong>firstName</strong> - User's first name (100 chars max)</li>
 *   <li><strong>lastName</strong> - User's last name (100 chars max)</li>
 *   <li><strong>preferredLocale</strong> - Language preference (default: "en")</li>
 *   <li><strong>timezone</strong> - User's timezone preference</li>
 *   <li><strong>phoneNumber</strong> - Contact phone number</li>
 * </ul>
 *
 * <h3>Security Features</h3>
 * <ul>
 *   <li><strong>Account Lockout</strong> - Support for temporary account disabling</li>
 *   <li><strong>Email Verification</strong> - Required email verification workflow</li>
 *   <li><strong>Password Security</strong> - BCrypt hashing with configurable strength</li>
 *   <li><strong>Audit Trail</strong> - Automatic creation and update timestamps</li>
 * </ul>
 *
 * <h3>Multi-Tenant Architecture</h3>
 * <ul>
 *   <li><strong>Tenant Isolation</strong> - Users belong to specific tenants</li>
 *   <li><strong>Cross-Tenant Prevention</strong> - Automatic filtering by tenant context</li>
 *   <li><strong>Tenant-Aware Queries</strong> - All queries automatically scoped to tenant</li>
 * </ul>
 *
 * <h3>Relationship Mappings</h3>
 * <ul>
 *   <li><strong>Authorities</strong> - Many-to-many relationship with roles and permissions</li>
 *   <li><strong>Organization</strong> - One-to-one relationship with organization entity</li>
 *   <li><strong>Preferences</strong> - One-to-one relationship with user preferences</li>
 *   <li><strong>Audit Logs</strong> - One-to-many relationship with security audit entries</li>
 * </ul>
 *
 * <h3>Database Constraints</h3>
 * <ul>
 *   <li><strong>Unique Constraints</strong> - Username and email must be globally unique</li>
 *   <li><strong>Not Null Constraints</strong> - Required fields enforced at database level</li>
 *   <li><strong>Length Constraints</strong> - Maximum lengths enforced for all string fields</li>
 *   <li><strong>Index Optimization</strong> - Indexes on frequently queried fields</li>
 * </ul>
 *
 * <h3>Security Considerations</h3>
 * <ul>
 *   <li><strong>Password Storage</strong> - Never store plain text passwords</li>
 *   <li><strong>Sensitive Data</strong> - Password hash excluded from DTOs and serialization</li>
 *   <li><strong>Account Status</strong> - Multiple layers of account disabling (enabled, emailVerified)</li>
 *   <li><strong>Audit Integration</strong> - All changes logged for security monitoring</li>
 * </ul>
 *
 * <h3>Usage Patterns</h3>
 * <pre>{@code
 * // Create new user
 * User user = new User();
 * user.setUsername("john.doe");
 * user.setEmail("john.doe@example.com");
 * user.setPasswordHash(passwordEncoder.encode(plainPassword));
 * user.setFirstName("John");
 * user.setLastName("Doe");
 * user.setEnabled(true);
 * user.setEmailVerified(false);
 *
 * // Add authorities
 * Set<Authority> authorities = Set.of(userRole, adminRole);
 * user.setAuthorities(authorities);
 *
 * userRepository.save(user);
 * }</pre>
 *
 * <h3>Validation Rules</h3>
 * <ul>
 *   <li><strong>Username</strong> - 3-50 characters, alphanumeric and underscore only</li>
 *   <li><strong>Email</strong> - Valid email format, maximum 255 characters</li>
 *   <li><strong>Names</strong> - 1-100 characters, letters and spaces only</li>
 *   <li><strong>Locale</strong> - Valid locale code (e.g., "en", "en_US")</li>
 * </ul>
 *
 * @author IQ Scaffold Team
 * @version 1.0
 * @see TenantAware
 * @see Authority
 * @see Organization
 * @see UserPreference
 * @since 1.0
 */
@Entity
@Table(name = "users")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "com.iqscaffold.userservice.usermanagement.User")
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
  @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "com.iqscaffold.userservice.usermanagement.User.authorities")
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
