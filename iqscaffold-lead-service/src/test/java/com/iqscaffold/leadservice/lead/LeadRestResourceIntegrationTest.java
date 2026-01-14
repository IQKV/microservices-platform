package com.iqscaffold.leadservice.lead;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqscaffold.leadservice.lead.dto.LeadDtos;
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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Lead CRUD operations.
 * Tests Requirements: 1.1, 1.2, 1.3, 1.4
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LeadRestResourceIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private LeadRepository leadRepository;

  @BeforeEach
  void setUp() {
    leadRepository.deleteAll();
  }

  /**
   * Test create lead with valid data.
   * Requirement 1.1: WHEN a user creates a lead with name, email, phone, company, and source,
   * THE CRM_System SHALL persist the lead with a unique identifier and creation timestamp
   */
  @Test
  @DisplayName("Should create lead with valid data")
  @WithMockUser(authorities = {"USER"})
  void testCreateLeadWithValidData() throws Exception {
    // Given
    LeadDtos.CreateLeadRequest request = new LeadDtos.CreateLeadRequest(
        "John",
        "Doe",
        "john.doe@example.com",
        "+1234567890",
        "Acme Corp",
        "Software Engineer",
        "Website",
        "Interested in our product",
        null
    );

    // When & Then
    mockMvc.perform(post("/api/v1/leads")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.firstName", is("John")))
        .andExpect(jsonPath("$.lastName", is("Doe")))
        .andExpect(jsonPath("$.email", is("john.doe@example.com")))
        .andExpect(jsonPath("$.phone", is("+1234567890")))
        .andExpect(jsonPath("$.company", is("Acme Corp")))
        .andExpect(jsonPath("$.jobTitle", is("Software Engineer")))
        .andExpect(jsonPath("$.source", is("Website")))
        .andExpect(jsonPath("$.status", is("NEW")))
        .andExpect(jsonPath("$.createdAt", notNullValue()))
        .andExpect(jsonPath("$.updatedAt", notNullValue()));
  }

  /**
   * Test get lead by ID.
   * Requirement 1.2: WHEN a user requests a lead by ID,
   * THE CRM_System SHALL return the complete lead information including all fields and metadata
   */
  @Test
  @DisplayName("Should get lead by ID")
  @WithMockUser(authorities = {"USER"})
  void testGetLeadById() throws Exception {
    // Given - Create a lead first
    Lead lead = new Lead("Jane", "Smith", "jane.smith@example.com", "Referral");
    lead.setPhone("+9876543210");
    lead.setCompany("Tech Solutions");
    lead.setJobTitle("CTO");
    lead.setNotes("High priority lead");
    lead.setCreatedBy("test-user");
    lead.setUpdatedBy("test-user");
    Lead savedLead = leadRepository.save(lead);

    // When & Then
    mockMvc.perform(get("/api/v1/leads/{id}", savedLead.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(savedLead.getId().intValue())))
        .andExpect(jsonPath("$.firstName", is("Jane")))
        .andExpect(jsonPath("$.lastName", is("Smith")))
        .andExpect(jsonPath("$.email", is("jane.smith@example.com")))
        .andExpect(jsonPath("$.phone", is("+9876543210")))
        .andExpect(jsonPath("$.company", is("Tech Solutions")))
        .andExpect(jsonPath("$.jobTitle", is("CTO")))
        .andExpect(jsonPath("$.source", is("Referral")))
        .andExpect(jsonPath("$.status", is("NEW")))
        .andExpect(jsonPath("$.notes", is("High priority lead")))
        .andExpect(jsonPath("$.createdAt", notNullValue()))
        .andExpect(jsonPath("$.updatedAt", notNullValue()));
  }

  /**
   * Test get lead by ID - not found scenario.
   * Requirement 1.2: Should return 404 when lead doesn't exist
   */
  @Test
  @DisplayName("Should return 404 when lead not found")
  @WithMockUser(authorities = {"USER"})
  void testGetLeadByIdNotFound() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/v1/leads/{id}", 99999L)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  /**
   * Test update lead.
   * Requirement 1.3: WHEN a user updates a lead's information,
   * THE CRM_System SHALL save the changes and update the modification timestamp
   */
  @Test
  @DisplayName("Should update lead with valid data")
  @WithMockUser(authorities = {"USER"})
  void testUpdateLead() throws Exception {
    // Given - Create a lead first
    Lead lead = new Lead("Bob", "Johnson", "bob.johnson@example.com", "Cold Call");
    lead.setPhone("+1111111111");
    lead.setCompany("Old Company");
    lead.setCreatedBy("test-user");
    lead.setUpdatedBy("test-user");
    Lead savedLead = leadRepository.save(lead);

    // Update request
    LeadDtos.UpdateLeadRequest updateRequest = new LeadDtos.UpdateLeadRequest(
        "Bob",
        "Johnson",
        "bob.johnson@example.com",
        "+2222222222",
        "New Company",
        "VP of Sales",
        "Cold Call",
        "Updated notes",
        null
    );

    // When & Then
    mockMvc.perform(put("/api/v1/leads/{id}", savedLead.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(savedLead.getId().intValue())))
        .andExpect(jsonPath("$.firstName", is("Bob")))
        .andExpect(jsonPath("$.lastName", is("Johnson")))
        .andExpect(jsonPath("$.email", is("bob.johnson@example.com")))
        .andExpect(jsonPath("$.phone", is("+2222222222")))
        .andExpect(jsonPath("$.company", is("New Company")))
        .andExpect(jsonPath("$.jobTitle", is("VP of Sales")))
        .andExpect(jsonPath("$.notes", is("Updated notes")))
        .andExpect(jsonPath("$.updatedAt", notNullValue()));
  }

  /**
   * Test update lead - not found scenario.
   * Requirement 1.3: Should return 404 when trying to update non-existent lead
   */
  @Test
  @DisplayName("Should return 404 when updating non-existent lead")
  @WithMockUser(authorities = {"USER"})
  void testUpdateLeadNotFound() throws Exception {
    // Given
    LeadDtos.UpdateLeadRequest updateRequest = new LeadDtos.UpdateLeadRequest(
        "Test",
        "User",
        "test@example.com",
        null,
        null,
        null,
        "Website",
        null,
        null
    );

    // When & Then
    mockMvc.perform(put("/api/v1/leads/{id}", 99999L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isNotFound());
  }

  /**
   * Test delete lead.
   * Requirement 1.4: WHEN a user deletes a lead,
   * THE CRM_System SHALL remove the lead from the active database and log the deletion in the Activity_Log
   */
  @Test
  @DisplayName("Should delete lead successfully")
  @WithMockUser(authorities = {"ADMIN"})
  void testDeleteLead() throws Exception {
    // Given - Create a lead first
    Lead lead = new Lead("Alice", "Williams", "alice.williams@example.com", "Email Campaign");
    lead.setCreatedBy("test-user");
    lead.setUpdatedBy("test-user");
    Lead savedLead = leadRepository.save(lead);

    // When & Then - Delete the lead
    mockMvc.perform(delete("/api/v1/leads/{id}", savedLead.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    // Verify lead is deleted
    mockMvc.perform(get("/api/v1/leads/{id}", savedLead.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  /**
   * Test delete lead - not found scenario.
   * Requirement 1.4: Should return 404 when trying to delete non-existent lead
   */
  @Test
  @DisplayName("Should return 404 when deleting non-existent lead")
  @WithMockUser(authorities = {"ADMIN"})
  void testDeleteLeadNotFound() throws Exception {
    // When & Then
    mockMvc.perform(delete("/api/v1/leads/{id}", 99999L)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  /**
   * Test delete lead - authorization check.
   * Requirement 1.4: Only ADMIN or SUPER_ADMIN can delete leads
   */
  @Test
  @DisplayName("Should return 403 when USER tries to delete lead")
  @WithMockUser(authorities = {"USER"})
  void testDeleteLeadForbiddenForUser() throws Exception {
    // Given - Create a lead first
    Lead lead = new Lead("Test", "Lead", "test.lead@example.com", "Website");
    lead.setCreatedBy("test-user");
    lead.setUpdatedBy("test-user");
    Lead savedLead = leadRepository.save(lead);

    // When & Then - USER role should not be able to delete
    mockMvc.perform(delete("/api/v1/leads/{id}", savedLead.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }
}
