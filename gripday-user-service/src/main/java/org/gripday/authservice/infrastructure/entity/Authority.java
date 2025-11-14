package org.gripday.authservice.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;

/**
 * Authority entity representing roles and permissions in the RBAC system. Used for role-based access control across the platform.
 */
@Entity
@Table(name = "authorities")
public class Authority {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "name", nullable = false, unique = true, length = 50)
  private String name;

  @Column(name = "description", length = 255)
  private String description;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @ManyToMany(mappedBy = "authorities", fetch = FetchType.LAZY)
  private Set<User> users = new HashSet<>();

  // Default constructor for JPA
  protected Authority() {
  }

  // Constructor with required fields
  public Authority(final String name) {
    this.name = name;
  }

  // Constructor with name and description
  public Authority(final String name, final String description) {
    this.name = name;
    this.description = description;
  }

  // Getters and setters
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

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public Set<User> getUsers() {
    return users;
  }

  public void setUsers(Set<User> users) {
    this.users = users;
  }

  // Utility methods using Java 21 features
  public void addUser(User user) {
    var currentUsers = this.users;
    currentUsers.add(user);
    user.getAuthorities().add(this);
  }

  public void removeUser(User user) {
    var currentUsers = this.users;
    currentUsers.remove(user);
    user.getAuthorities().remove(this);
  }

  public boolean hasUser(User user) {
    var users = this.users;
    return users.contains(user);
  }

  public int getUserCount() {
    var users = this.users;
    return users.size();
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

    var authority = (Authority) obj;
    return Objects.equals(id, authority.id)
        && Objects.equals(name, authority.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }

  @Override
  public String toString() {
    var sb = new StringBuilder();
    sb.append("Authority{")
        .append("id=").append(id)
        .append(", name='").append(name).append('\'')
        .append(", description='").append(description).append('\'')
        .append(", createdAt=").append(createdAt)
        .append(", userCount=").append(users.size())
        .append('}');
    return sb.toString();
  }
}