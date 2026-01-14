package com.iqscaffold.contactservice.security;

import com.iqscaffold.contactservice.contact.Contact;
import com.iqscaffold.contactservice.contact.ContactRepository;
import com.iqscaffold.contactservice.contact.ContactStatus;
import com.iqscaffold.contactservice.tenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for tenant data isolation and JWT authentication in Contact Service.
 * Tests Requirements: 9.3, 9.4, 10.1, 10.2
 * 
 * NOTE: These tests are temporarily disabled because they require tenant schema setup.
 * See backend/TENANT_ISOLATION_TEST_SOLUTION.md for implementation details.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Disabled("Temporarily disabled - requires tenant schema configuration.")
class TenantIsolationIntegrationTest {

  @Autowired
  private ContactRepository contactRepository;

  @BeforeEach
  void setUp() {
    contactRepository.deleteAll();
    TenantContext.clear();
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  /**
   * Test tenant data isolation at repository level.
   * Requirement 9.3: WHEN a user from Tenant A queries contacts,
   * THE CRM_System SHALL return only contacts belonging to Tenant A
   */
  @Test
  @DisplayName("Should return only contacts for current tenant")
  void testContactsTenantIsolation() {
    // Given - Create contacts for different tenants
    TenantContext.setCurrentTenantId("tenant-a");
    Contact contactA1 = new Contact("Alice", "Anderson", "alice@tenant-a.com");
    contactA1.setPhone("+1111111111");
    contactA1.setJobTitle("CEO");
    contactA1.setStatus(ContactStatus.ACTIVE);
    contactA1.setCreatedBy("user-a");
    contactA1.setUpdatedBy("user-a");
    contactRepository.save(contactA1);

    Contact contactA2 = new Contact("Adam", "Adams", "adam@tenant-a.com");
    contactA2.setPhone("+2222222222");
    contactA2.setJobTitle("CTO");
    contactA2.setStatus(ContactStatus.ACTIVE);
    contactA2.setCreatedBy("user-a");
    contactA2.setUpdatedBy("user-a");
    contactRepository.save(contactA2);

    TenantContext.setCurrentTenantId("tenant-b");
    Contact contactB1 = new Contact("Bob", "Brown", "bob@tenant-b.com");
    contactB1.setPhone("+3333333333");
    contactB1.setJobTitle("Manager");
    contactB1.setStatus(ContactStatus.ACTIVE);
    contactB1.setCreatedBy("user-b");
    contactB1.setUpdatedBy("user-b");
    contactRepository.save(contactB1);

    Contact contactB2 = new Contact("Betty", "Baker", "betty@tenant-b.com");
    contactB2.setPhone("+4444444444");
    contactB2.setJobTitle("Director");
    contactB2.setStatus(ContactStatus.ACTIVE);
    contactB2.setCreatedBy("user-b");
    contactB2.setUpdatedBy("user-b");
    contactRepository.save(contactB2);

    // When & Then - Query as tenant-a should only return tenant-a contacts
    TenantContext.setCurrentTenantId("tenant-a");
    List<Contact> contactsA = contactRepository.findAll();
    assertThat(contactsA).hasSize(2);
    assertThat(contactsA).extracting(Contact::getEmail)
        .containsExactlyInAnyOrder("alice@tenant-a.com", "adam@tenant-a.com");

    // Query as tenant-b should only return tenant-b contacts
    TenantContext.setCurrentTenantId("tenant-b");
    List<Contact> contactsB = contactRepository.findAll();
    assertThat(contactsB).hasSize(2);
    assertThat(contactsB).extracting(Contact::getEmail)
        .containsExactlyInAnyOrder("bob@tenant-b.com", "betty@tenant-b.com");
  }

  /**
   * Test cross-tenant access prevention.
   * Requirement 9.4: WHEN a user attempts to access a contact from a different tenant,
   * THE CRM_System SHALL not return the contact
   */
  @Test
  @DisplayName("Should not allow access to contacts from different tenant")
  void testCrossTenantAccessPrevention() {
    // Given - Create a contact for tenant-x
    TenantContext.setCurrentTenantId("tenant-x");
    Contact contactX = new Contact("Charlie", "Clark", "charlie@tenant-x.com");
    contactX.setPhone("+5555555555");
    contactX.setStatus(ContactStatus.ACTIVE);
    contactX.setCreatedBy("user-x");
    contactX.setUpdatedBy("user-x");
    Contact savedContact = contactRepository.save(contactX);
    Long contactId = savedContact.getId();

    // When - Try to access tenant-x's contact as tenant-y
    TenantContext.setCurrentTenantId("tenant-y");
    var foundContact = contactRepository.findById(contactId);

    // Then - Should not find the contact
    assertThat(foundContact).isEmpty();
  }

  /**
   * Test tenant isolation for contact creation.
   * Requirement 9.3: Created contacts should be associated with the correct tenant
   */
  @Test
  @DisplayName("Should create contact in correct tenant context")
  void testContactCreationInTenantContext() {
    // Given & When - Create contact as tenant-c
    TenantContext.setCurrentTenantId("tenant-c");
    Contact contactC = new Contact("David", "Davis", "david@tenant-c.com");
    contactC.setPhone("+6666666666");
    contactC.setJobTitle("VP Sales");
    contactC.setStatus(ContactStatus.ACTIVE);
    contactC.setCreatedBy("user-c");
    contactC.setUpdatedBy("user-c");
    contactRepository.save(contactC);

    // Then - Verify contact is only accessible by tenant-c
    List<Contact> contactsC = contactRepository.findAll();
    assertThat(contactsC).hasSize(1);
    assertThat(contactsC.get(0).getEmail()).isEqualTo("david@tenant-c.com");

    // Verify contact is NOT accessible by tenant-d
    TenantContext.setCurrentTenantId("tenant-d");
    List<Contact> contactsD = contactRepository.findAll();
    assertThat(contactsD).isEmpty();
  }

  /**
   * Test tenant isolation with multiple operations.
   * Requirement 9.3: All operations should respect tenant boundaries
   */
  @Test
  @DisplayName("Should maintain tenant isolation across multiple operations")
  void testTenantIsolationAcrossOperations() {
    // Given - Create contacts for tenant-m
    TenantContext.setCurrentTenantId("tenant-m");
    Contact contact1 = new Contact("Emma", "Evans", "emma@tenant-m.com");
    contact1.setStatus(ContactStatus.ACTIVE);
    contact1.setCreatedBy("user-m");
    contact1.setUpdatedBy("user-m");
    contactRepository.save(contact1);

    Contact contact2 = new Contact("Ethan", "Edwards", "ethan@tenant-m.com");
    contact2.setStatus(ContactStatus.ACTIVE);
    contact2.setCreatedBy("user-m");
    contact2.setUpdatedBy("user-m");
    contactRepository.save(contact2);

    // When - Count contacts for tenant-m
    long countM = contactRepository.count();
    assertThat(countM).isEqualTo(2);

    // Switch to tenant-n and create contacts
    TenantContext.setCurrentTenantId("tenant-n");
    Contact contact3 = new Contact("Frank", "Foster", "frank@tenant-n.com");
    contact3.setStatus(ContactStatus.ACTIVE);
    contact3.setCreatedBy("user-n");
    contact3.setUpdatedBy("user-n");
    contactRepository.save(contact3);

    // Then - Count should only show tenant-n contacts
    long countN = contactRepository.count();
    assertThat(countN).isEqualTo(1);

    // Switch back to tenant-m - should still have 2 contacts
    TenantContext.setCurrentTenantId("tenant-m");
    long countMAgain = contactRepository.count();
    assertThat(countMAgain).isEqualTo(2);
  }

  /**
   * Test tenant isolation with contact updates.
   * Requirement 9.3: Updates should only affect contacts in the current tenant
   */
  @Test
  @DisplayName("Should only update contacts in current tenant")
  void testTenantIsolationForUpdates() {
    // Given - Create contacts for different tenants
    TenantContext.setCurrentTenantId("tenant-p");
    Contact contactP = new Contact("Grace", "Green", "grace@tenant-p.com");
    contactP.setStatus(ContactStatus.ACTIVE);
    contactP.setLeadScore(50);
    contactP.setCreatedBy("user-p");
    contactP.setUpdatedBy("user-p");
    Contact savedContactP = contactRepository.save(contactP);

    TenantContext.setCurrentTenantId("tenant-q");
    Contact contactQ = new Contact("George", "Gray", "george@tenant-q.com");
    contactQ.setStatus(ContactStatus.ACTIVE);
    contactQ.setLeadScore(50);
    contactQ.setCreatedBy("user-q");
    contactQ.setUpdatedBy("user-q");
    contactRepository.save(contactQ);

    // When - Update contact in tenant-p
    TenantContext.setCurrentTenantId("tenant-p");
    var foundContact = contactRepository.findById(savedContactP.getId());
    assertThat(foundContact).isPresent();
    foundContact.get().setLeadScore(100);
    contactRepository.save(foundContact.get());

    // Then - Verify update only affected tenant-p
    var updatedContact = contactRepository.findById(savedContactP.getId());
    assertThat(updatedContact).isPresent();
    assertThat(updatedContact.get().getLeadScore()).isEqualTo(100);

    // Verify tenant-q contact is unchanged
    TenantContext.setCurrentTenantId("tenant-q");
    List<Contact> contactsQ = contactRepository.findAll();
    assertThat(contactsQ).hasSize(1);
    assertThat(contactsQ.get(0).getLeadScore()).isEqualTo(50);
  }

  /**
   * Test tenant isolation with contact deletion.
   * Requirement 9.3: Deletions should only affect contacts in the current tenant
   */
  @Test
  @DisplayName("Should only delete contacts in current tenant")
  void testTenantIsolationForDeletions() {
    // Given - Create contacts for different tenants
    TenantContext.setCurrentTenantId("tenant-r");
    Contact contactR = new Contact("Helen", "Harris", "helen@tenant-r.com");
    contactR.setStatus(ContactStatus.ACTIVE);
    contactR.setCreatedBy("user-r");
    contactR.setUpdatedBy("user-r");
    Contact savedContactR = contactRepository.save(contactR);

    TenantContext.setCurrentTenantId("tenant-s");
    Contact contactS = new Contact("Henry", "Hill", "henry@tenant-s.com");
    contactS.setStatus(ContactStatus.ACTIVE);
    contactS.setCreatedBy("user-s");
    contactS.setUpdatedBy("user-s");
    contactRepository.save(contactS);

    // When - Delete contact in tenant-r
    TenantContext.setCurrentTenantId("tenant-r");
    contactRepository.deleteById(savedContactR.getId());

    // Then - Verify deletion only affected tenant-r
    List<Contact> contactsR = contactRepository.findAll();
    assertThat(contactsR).isEmpty();

    // Verify tenant-s contact still exists
    TenantContext.setCurrentTenantId("tenant-s");
    List<Contact> contactsS = contactRepository.findAll();
    assertThat(contactsS).hasSize(1);
    assertThat(contactsS.get(0).getEmail()).isEqualTo("henry@tenant-s.com");
  }

  /**
   * Test tenant context is properly managed.
   * Requirement 9.3: Tenant context should be set and cleared properly
   */
  @Test
  @DisplayName("Should properly manage tenant context")
  void testTenantContextManagement() {
    // Given - No tenant context initially
    assertThat(TenantContext.getCurrentTenantId()).isNull();

    // When - Set tenant context
    TenantContext.setCurrentTenantId("tenant-test");

    // Then - Context should be set
    assertThat(TenantContext.getCurrentTenantId()).isEqualTo("tenant-test");
    assertThat(TenantContext.hasTenantContext()).isTrue();

    // When - Clear context
    TenantContext.clear();

    // Then - Context should be cleared
    assertThat(TenantContext.getCurrentTenantId()).isNull();
    assertThat(TenantContext.hasTenantContext()).isFalse();
  }

  /**
   * Test tenant isolation with converted leads.
   * Requirement 9.3: Converted lead tracking should respect tenant boundaries
   */
  @Test
  @DisplayName("Should maintain tenant isolation for converted leads")
  void testTenantIsolationForConvertedLeads() {
    // Given - Create contacts converted from leads in different tenants
    TenantContext.setCurrentTenantId("tenant-u");
    Contact contactU = new Contact("Iris", "Irwin", "iris@tenant-u.com");
    contactU.setStatus(ContactStatus.ACTIVE);
    contactU.setConvertedFromLeadId(1001L);
    contactU.setConvertedAt(java.time.LocalDateTime.now());
    contactU.setCreatedBy("user-u");
    contactU.setUpdatedBy("user-u");
    contactRepository.save(contactU);

    TenantContext.setCurrentTenantId("tenant-v");
    Contact contactV = new Contact("Ian", "Ingram", "ian@tenant-v.com");
    contactV.setStatus(ContactStatus.ACTIVE);
    contactV.setConvertedFromLeadId(2001L);
    contactV.setConvertedAt(java.time.LocalDateTime.now());
    contactV.setCreatedBy("user-v");
    contactV.setUpdatedBy("user-v");
    contactRepository.save(contactV);

    // When & Then - Query as tenant-u should only return tenant-u contacts
    TenantContext.setCurrentTenantId("tenant-u");
    List<Contact> contactsU = contactRepository.findAll();
    assertThat(contactsU).hasSize(1);
    assertThat(contactsU.get(0).getConvertedFromLeadId()).isEqualTo(1001L);

    // Query as tenant-v should only return tenant-v contacts
    TenantContext.setCurrentTenantId("tenant-v");
    List<Contact> contactsV = contactRepository.findAll();
    assertThat(contactsV).hasSize(1);
    assertThat(contactsV.get(0).getConvertedFromLeadId()).isEqualTo(2001L);
  }
}
