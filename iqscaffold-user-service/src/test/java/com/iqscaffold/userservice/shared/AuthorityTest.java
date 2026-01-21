package com.iqscaffold.userservice.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.iqscaffold.userservice.usermanagement.User;


class AuthorityTest {

  @Test
  void addAndRemoveUserShouldMaintainBidirectionalRelationship() {
    var user = new User("u1", "u1@example.com", "hash", "U", "One", "tenant-1");
    var admin = new Authority("ADMIN");

    assertEquals(0, admin.getUserCount());
    assertFalse(admin.hasUser(user));
    assertFalse(user.hasAuthority("ADMIN"));

    admin.addUser(user);

    assertEquals(1, admin.getUserCount());
    assertTrue(admin.hasUser(user));
    assertTrue(user.hasAuthority("ADMIN"));

    admin.removeUser(user);

    assertEquals(0, admin.getUserCount());
    assertFalse(admin.hasUser(user));
    assertFalse(user.hasAuthority("ADMIN"));
  }

  @Test
  void equalsAndHashCodeShouldConsiderIdAndName() {
    var a1 = new Authority("USER");
    var a2 = new Authority("USER");

    // Without IDs set, name equality still considered in equals/hashCode per implementation
    assertEquals(a1, a2);
    assertEquals(a1.hashCode(), a2.hashCode());

    var a3 = new Authority("ADMIN");
    assertNotEquals(a1, a3);
  }
}
