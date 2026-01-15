package com.iqscaffold.contactservice.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqscaffold.contactservice.contact.dto.ContactDtos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for Contact bulk operations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContactBulkOperationsIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ContactRepository contactRepository;

  @BeforeEach
  void setUp() {
    contactRepository.deleteAll();
  }

  @Test
  @DisplayName("Should bulk create contacts")
  @WithMockUser(authorities = "USER")
  void shouldBulkCreateContacts() throws Exception {
    List<ContactDtos.CreateContactRequest> contacts = Arrays.asList(
        new ContactDtos.CreateContactRequest("John", "Doe", "john@example.com", null, null, null, ContactStatus.ACTIVE, null, null),
        new ContactDtos.CreateContactRequest("Jane", "Smith", "jane@example.com", null, null, null, ContactStatus.ACTIVE, null, null),
        new ContactDtos.CreateContactRequest("Bob", "Johnson", "bob@example.com", null, null, null, ContactStatus.ACTIVE, null, null)
    );

    ContactDtos.BulkCreateContactsRequest request = new ContactDtos.BulkCreateContactsRequest(contacts);

    mockMvc.perform(post("/api/v1/contacts/bulk")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.successCount").value(3))
        .andExpect(jsonPath("$.failureCount").value(0))
        .andExpect(jsonPath("$.results.length()").value(3));

    assertThat(contactRepository.count()).isEqualTo(3);
  }

  @Test
  @DisplayName("Should handle duplicate emails in bulk create")
  @WithMockUser(authorities = "USER")
  void shouldHandleDuplicateEmailsInBulkCreate() throws Exception {
    // Create existing contact
    Contact existing = new Contact();
    existing.setFirstName("Existing");
    existing.setLastName("Contact");
    existing.setEmail("existing@example.com");
    existing.setStatus(ContactStatus.ACTIVE);
    existing.setLeadScore(0);
    existing.setCreatedBy("test");
    existing.setUpdatedBy("test");
    contactRepository.save(existing);

    List<ContactDtos.CreateContactRequest> contacts = Arrays.asList(
        new ContactDtos.CreateContactRequest("John", "Doe", "john@example.com", null, null, null, ContactStatus.ACTIVE, null, null),
        new ContactDtos.CreateContactRequest("Duplicate", "Contact", "existing@example.com", null, null, null, ContactStatus.ACTIVE, null, null),
        new ContactDtos.CreateContactRequest("Jane", "Smith", "jane@example.com", null, null, null, ContactStatus.ACTIVE, null, null)
    );

    ContactDtos.BulkCreateContactsRequest request = new ContactDtos.BulkCreateContactsRequest(contacts);

    mockMvc.perform(post("/api/v1/contacts/bulk")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.successCount").value(2))
        .andExpect(jsonPath("$.failureCount").value(1))
        .andExpect(jsonPath("$.results.length()").value(3));

    assertThat(contactRepository.count()).isEqualTo(3); // 1 existing + 2 new
  }

  @Test
  @DisplayName("Should bulk update contact status")
  @WithMockUser(authorities = "USER")
  void shouldBulkUpdateContactStatus() throws Exception {
    Contact contact1 = createTestContact("John", "Doe", "john@example.com");
    Contact contact2 = createTestContact("Jane", "Smith", "jane@example.com");
    Contact contact3 = createTestContact("Bob", "Johnson", "bob@example.com");

    ContactDtos.BulkUpdateStatusRequest request = new ContactDtos.BulkUpdateStatusRequest(
        Arrays.asList(contact1.getId(), contact2.getId(), contact3.getId()),
        ContactStatus.CUSTOMER
    );

    mockMvc.perform(patch("/api/v1/contacts/bulk/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.successCount").value(3))
        .andExpect(jsonPath("$.failureCount").value(0));

    Contact updated1 = contactRepository.findById(contact1.getId()).orElseThrow();
    Contact updated2 = contactRepository.findById(contact2.getId()).orElseThrow();
    Contact updated3 = contactRepository.findById(contact3.getId()).orElseThrow();

    assertThat(updated1.getStatus()).isEqualTo(ContactStatus.CUSTOMER);
    assertThat(updated2.getStatus()).isEqualTo(ContactStatus.CUSTOMER);
    assertThat(updated3.getStatus()).isEqualTo(ContactStatus.CUSTOMER);
  }

  @Test
  @DisplayName("Should handle non-existent contacts in bulk status update")
  @WithMockUser(authorities = "USER")
  void shouldHandleNonExistentContactsInBulkStatusUpdate() throws Exception {
    Contact contact1 = createTestContact("John", "Doe", "john@example.com");

    ContactDtos.BulkUpdateStatusRequest request = new ContactDtos.BulkUpdateStatusRequest(
        Arrays.asList(contact1.getId(), 99999L, 99998L),
        ContactStatus.CUSTOMER
    );

    mockMvc.perform(patch("/api/v1/contacts/bulk/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.successCount").value(1))
        .andExpect(jsonPath("$.failureCount").value(2));
  }

  @Test
  @DisplayName("Should bulk delete contacts")
  @WithMockUser(authorities = "ADMIN")
  void shouldBulkDeleteContacts() throws Exception {
    Contact contact1 = createTestContact("John", "Doe", "john@example.com");
    Contact contact2 = createTestContact("Jane", "Smith", "jane@example.com");
    Contact contact3 = createTestContact("Bob", "Johnson", "bob@example.com");

    ContactDtos.BulkDeleteContactsRequest request = new ContactDtos.BulkDeleteContactsRequest(
        Arrays.asList(contact1.getId(), contact2.getId(), contact3.getId())
    );

    mockMvc.perform(delete("/api/v1/contacts/bulk")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.successCount").value(3))
        .andExpect(jsonPath("$.failureCount").value(0));

    assertThat(contactRepository.count()).isEqualTo(0);
  }

  @Test
  @DisplayName("Should deny bulk delete for non-admin user")
  @WithMockUser(authorities = "USER")
  void shouldDenyBulkDeleteForNonAdminUser() throws Exception {
    Contact contact = createTestContact("John", "Doe", "john@example.com");

    ContactDtos.BulkDeleteContactsRequest request = new ContactDtos.BulkDeleteContactsRequest(
        Arrays.asList(contact.getId())
    );

    mockMvc.perform(delete("/api/v1/contacts/bulk")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Should bulk update lead scores")
  @WithMockUser(authorities = "USER")
  void shouldBulkUpdateLeadScores() throws Exception {
    Contact contact1 = createTestContact("John", "Doe", "john@example.com");
    Contact contact2 = createTestContact("Jane", "Smith", "jane@example.com");

    List<ContactDtos.BulkUpdateLeadScoresRequest.LeadScoreUpdate> updates = Arrays.asList(
        new ContactDtos.BulkUpdateLeadScoresRequest.LeadScoreUpdate(contact1.getId(), 85),
        new ContactDtos.BulkUpdateLeadScoresRequest.LeadScoreUpdate(contact2.getId(), 90)
    );

    ContactDtos.BulkUpdateLeadScoresRequest request = new ContactDtos.BulkUpdateLeadScoresRequest(updates);

    mockMvc.perform(patch("/api/v1/contacts/bulk/scores")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.successCount").value(2))
        .andExpect(jsonPath("$.failureCount").value(0));

    Contact updated1 = contactRepository.findById(contact1.getId()).orElseThrow();
    Contact updated2 = contactRepository.findById(contact2.getId()).orElseThrow();

    assertThat(updated1.getLeadScore()).isEqualTo(85);
    assertThat(updated2.getLeadScore()).isEqualTo(90);
  }

  @Test
  @DisplayName("Should enforce maximum bulk operation size")
  @WithMockUser(authorities = "USER")
  void shouldEnforceMaximumBulkOperationSize() throws Exception {
    List<ContactDtos.CreateContactRequest> contacts = new ArrayList<>();
    for (int i = 0; i < 101; i++) {
      contacts.add(new ContactDtos.CreateContactRequest(
          "Contact" + i,
          "Test",
          "contact" + i + "@example.com",
          null, null, null, ContactStatus.ACTIVE, null, null
      ));
    }

    ContactDtos.BulkCreateContactsRequest request = new ContactDtos.BulkCreateContactsRequest(contacts);

    mockMvc.perform(post("/api/v1/contacts/bulk")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  private Contact createTestContact(String firstName, String lastName, String email) {
    Contact contact = new Contact();
    contact.setFirstName(firstName);
    contact.setLastName(lastName);
    contact.setEmail(email);
    contact.setStatus(ContactStatus.ACTIVE);
    contact.setLeadScore(0);
    contact.setCreatedBy("test");
    contact.setUpdatedBy("test");
    return contactRepository.save(contact);
  }
}
