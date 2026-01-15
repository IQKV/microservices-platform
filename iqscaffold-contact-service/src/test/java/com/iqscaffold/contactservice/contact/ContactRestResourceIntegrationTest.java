package com.iqscaffold.contactservice.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for Contact REST API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContactRestResourceIntegrationTest {

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
  @DisplayName("Should create contact successfully")
  @WithMockUser(authorities = "USER")
  void shouldCreateContact() throws Exception {
    ContactDtos.CreateContactRequest request = new ContactDtos.CreateContactRequest(
        "John",
        "Doe",
        "john.doe@example.com",
        "+1234567890",
        "Software Engineer",
        null,
        ContactStatus.ACTIVE,
        75,
        "Test contact"
    );

    mockMvc.perform(post("/api/v1/contacts")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.firstName").value("John"))
        .andExpect(jsonPath("$.lastName").value("Doe"))
        .andExpect(jsonPath("$.email").value("john.doe@example.com"))
        .andExpect(jsonPath("$.phone").value("+1234567890"))
        .andExpect(jsonPath("$.jobTitle").value("Software Engineer"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.leadScore").value(75))
        .andExpect(jsonPath("$.id").exists());

    assertThat(contactRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should fail to create contact with duplicate email")
  @WithMockUser(authorities = "USER")
  void shouldFailToCreateContactWithDuplicateEmail() throws Exception {
    // Create first contact
    Contact existingContact = new Contact();
    existingContact.setFirstName("Jane");
    existingContact.setLastName("Smith");
    existingContact.setEmail("jane.smith@example.com");
    existingContact.setStatus(ContactStatus.ACTIVE);
    existingContact.setLeadScore(0);
    existingContact.setCreatedBy("test");
    existingContact.setUpdatedBy("test");
    contactRepository.save(existingContact);

    // Try to create contact with same email
    ContactDtos.CreateContactRequest request = new ContactDtos.CreateContactRequest(
        "John",
        "Doe",
        "jane.smith@example.com",
        null,
        null,
        null,
        ContactStatus.ACTIVE,
        null,
        null
    );

    mockMvc.perform(post("/api/v1/contacts")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("Should get contact by ID")
  @WithMockUser(authorities = "USER")
  void shouldGetContactById() throws Exception {
    Contact contact = createTestContact("John", "Doe", "john.doe@example.com");

    mockMvc.perform(get("/api/v1/contacts/{id}", contact.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(contact.getId()))
        .andExpect(jsonPath("$.firstName").value("John"))
        .andExpect(jsonPath("$.lastName").value("Doe"))
        .andExpect(jsonPath("$.email").value("john.doe@example.com"));
  }

  @Test
  @DisplayName("Should return 404 for non-existent contact")
  @WithMockUser(authorities = "USER")
  void shouldReturn404ForNonExistentContact() throws Exception {
    mockMvc.perform(get("/api/v1/contacts/{id}", 99999L))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should list all contacts")
  @WithMockUser(authorities = "USER")
  void shouldListAllContacts() throws Exception {
    createTestContact("John", "Doe", "john.doe@example.com");
    createTestContact("Jane", "Smith", "jane.smith@example.com");
    createTestContact("Bob", "Johnson", "bob.johnson@example.com");

    mockMvc.perform(get("/api/v1/contacts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(3))
        .andExpect(jsonPath("$.totalElements").value(3));
  }

  @Test
  @DisplayName("Should search contacts by name")
  @WithMockUser(authorities = "USER")
  void shouldSearchContactsByName() throws Exception {
    createTestContact("John", "Doe", "john.doe@example.com");
    createTestContact("Jane", "Doe", "jane.doe@example.com");
    createTestContact("Bob", "Smith", "bob.smith@example.com");

    mockMvc.perform(get("/api/v1/contacts")
            .param("search", "Doe"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2));
  }

  @Test
  @DisplayName("Should filter contacts by status")
  @WithMockUser(authorities = "USER")
  void shouldFilterContactsByStatus() throws Exception {
    Contact contact1 = createTestContact("John", "Doe", "john.doe@example.com");
    contact1.setStatus(ContactStatus.ACTIVE);
    contactRepository.save(contact1);

    Contact contact2 = createTestContact("Jane", "Smith", "jane.smith@example.com");
    contact2.setStatus(ContactStatus.INACTIVE);
    contactRepository.save(contact2);

    mockMvc.perform(get("/api/v1/contacts")
            .param("status", "ACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
  }

  @Test
  @DisplayName("Should update contact")
  @WithMockUser(authorities = "USER")
  void shouldUpdateContact() throws Exception {
    Contact contact = createTestContact("John", "Doe", "john.doe@example.com");

    ContactDtos.UpdateContactRequest request = new ContactDtos.UpdateContactRequest(
        "John",
        "Smith",
        "john.smith@example.com",
        "+9876543210",
        "Senior Engineer",
        null,
        ContactStatus.CUSTOMER,
        "Updated notes"
    );

    mockMvc.perform(put("/api/v1/contacts/{id}", contact.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lastName").value("Smith"))
        .andExpect(jsonPath("$.email").value("john.smith@example.com"))
        .andExpect(jsonPath("$.phone").value("+9876543210"))
        .andExpect(jsonPath("$.jobTitle").value("Senior Engineer"))
        .andExpect(jsonPath("$.status").value("CUSTOMER"));
  }

  @Test
  @DisplayName("Should delete contact")
  @WithMockUser(authorities = "ADMIN")
  void shouldDeleteContact() throws Exception {
    Contact contact = createTestContact("John", "Doe", "john.doe@example.com");

    mockMvc.perform(delete("/api/v1/contacts/{id}", contact.getId()))
        .andExpect(status().isNoContent());

    assertThat(contactRepository.findById(contact.getId())).isEmpty();
  }

  @Test
  @DisplayName("Should deny delete for non-admin user")
  @WithMockUser(authorities = "USER")
  void shouldDenyDeleteForNonAdminUser() throws Exception {
    Contact contact = createTestContact("John", "Doe", "john.doe@example.com");

    mockMvc.perform(delete("/api/v1/contacts/{id}", contact.getId()))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Should get contacts by company")
  @WithMockUser(authorities = "USER")
  void shouldGetContactsByCompany() throws Exception {
    Contact contact1 = createTestContact("John", "Doe", "john.doe@example.com");
    contact1.setCompanyId(1L);
    contactRepository.save(contact1);

    Contact contact2 = createTestContact("Jane", "Smith", "jane.smith@example.com");
    contact2.setCompanyId(1L);
    contactRepository.save(contact2);

    Contact contact3 = createTestContact("Bob", "Johnson", "bob.johnson@example.com");
    contact3.setCompanyId(2L);
    contactRepository.save(contact3);

    mockMvc.perform(get("/api/v1/contacts/company/{companyId}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @DisplayName("Should update lead score")
  @WithMockUser(authorities = "USER")
  void shouldUpdateLeadScore() throws Exception {
    Contact contact = createTestContact("John", "Doe", "john.doe@example.com");

    ContactDtos.UpdateLeadScoreRequest request = new ContactDtos.UpdateLeadScoreRequest(85);

    mockMvc.perform(patch("/api/v1/contacts/{id}/score", contact.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.leadScore").value(85));
  }

  @Test
  @DisplayName("Should validate required fields")
  @WithMockUser(authorities = "USER")
  void shouldValidateRequiredFields() throws Exception {
    ContactDtos.CreateContactRequest request = new ContactDtos.CreateContactRequest(
        "",  // Empty first name
        "",  // Empty last name
        "invalid-email",  // Invalid email
        null,
        null,
        null,
        null,
        null,
        null
    );

    mockMvc.perform(post("/api/v1/contacts")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should paginate results")
  @WithMockUser(authorities = "USER")
  void shouldPaginateResults() throws Exception {
    for (int i = 0; i < 25; i++) {
      createTestContact("Contact" + i, "Test", "contact" + i + "@example.com");
    }

    mockMvc.perform(get("/api/v1/contacts")
            .param("page", "0")
            .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(10))
        .andExpect(jsonPath("$.totalElements").value(25))
        .andExpect(jsonPath("$.totalPages").value(3));
  }

  @Test
  @DisplayName("Should sort contacts")
  @WithMockUser(authorities = "USER")
  void shouldSortContacts() throws Exception {
    createTestContact("Charlie", "Brown", "charlie@example.com");
    createTestContact("Alice", "Smith", "alice@example.com");
    createTestContact("Bob", "Jones", "bob@example.com");

    mockMvc.perform(get("/api/v1/contacts")
            .param("sort", "firstName,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].firstName").value("Alice"))
        .andExpect(jsonPath("$.content[1].firstName").value("Bob"))
        .andExpect(jsonPath("$.content[2].firstName").value("Charlie"));
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
