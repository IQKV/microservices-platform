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

  /**
   * Test pagination.
   * Requirement 2.1: WHEN a user requests the lead list,
   * THE CRM_System SHALL return paginated results with default page size of 20
   */
  @Test
  @DisplayName("Should return paginated results with default page size")
  @WithMockUser(authorities = {"USER"})
  void testPagination() throws Exception {
    // Given - Create 25 leads
    for (int i = 1; i <= 25; i++) {
      Lead lead = new Lead("First" + i, "Last" + i, "email" + i + "@example.com", "Website");
      lead.setCreatedBy("test-user");
      lead.setUpdatedBy("test-user");
      leadRepository.save(lead);
    }

    // When & Then - Request first page with default size (20)
    mockMvc.perform(get("/api/v1/leads")
            .param("page", "0")
            .param("size", "20")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()", is(20)))
        .andExpect(jsonPath("$.totalElements", is(25)))
        .andExpect(jsonPath("$.totalPages", is(2)))
        .andExpect(jsonPath("$.number", is(0)))
        .andExpect(jsonPath("$.size", is(20)));

    // Request second page
    mockMvc.perform(get("/api/v1/leads")
            .param("page", "1")
            .param("size", "20")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()", is(5)))
        .andExpect(jsonPath("$.totalElements", is(25)))
        .andExpect(jsonPath("$.totalPages", is(2)))
        .andExpect(jsonPath("$.number", is(1)))
        .andExpect(jsonPath("$.size", is(20)));
  }

  /**
   * Test pagination with custom page size.
   * Requirement 2.7: WHEN a user requests a specific page and page size,
   * THE CRM_System SHALL return the correct subset of results
   */
  @Test
  @DisplayName("Should return paginated results with custom page size")
  @WithMockUser(authorities = {"USER"})
  void testPaginationWithCustomSize() throws Exception {
    // Given - Create 15 leads
    for (int i = 1; i <= 15; i++) {
      Lead lead = new Lead("First" + i, "Last" + i, "email" + i + "@example.com", "Website");
      lead.setCreatedBy("test-user");
      lead.setUpdatedBy("test-user");
      leadRepository.save(lead);
    }

    // When & Then - Request with page size of 5
    mockMvc.perform(get("/api/v1/leads")
            .param("page", "0")
            .param("size", "5")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()", is(5)))
        .andExpect(jsonPath("$.totalElements", is(15)))
        .andExpect(jsonPath("$.totalPages", is(3)))
        .andExpect(jsonPath("$.number", is(0)))
        .andExpect(jsonPath("$.size", is(5)));
  }

  /**
   * Test search by term.
   * Requirement 2.2: WHEN a user provides a search term,
   * THE CRM_System SHALL return leads matching the term in name, email, company, or phone fields
   */
  @Test
  @DisplayName("Should search leads by term matching name")
  @WithMockUser(authorities = {"USER"})
  void testSearchByTermMatchingName() throws Exception {
    // Given - Create leads with different names
    Lead lead1 = new Lead("John", "Smith", "john.smith@example.com", "Website");
    lead1.setCreatedBy("test-user");
    lead1.setUpdatedBy("test-user");
    leadRepository.save(lead1);

    Lead lead2 = new Lead("Jane", "Doe", "jane.doe@example.com", "Referral");
    lead2.setCreatedBy("test-user");
    lead2.setUpdatedBy("test-user");
    leadRepository.save(lead2);

    Lead lead3 = new Lead("Bob", "Johnson", "bob.johnson@example.com", "Cold Call");
    lead3.setCreatedBy("test-user");
    lead3.setUpdatedBy("test-user");
    leadRepository.save(lead3);

    // When & Then - Search for "John" should match "John Smith" and "Bob Johnson"
    mockMvc.perform(get("/api/v1/leads")
            .param("search", "John")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()", is(2)))
        .andExpect(jsonPath("$.totalElements", is(2)));
  }

  /**
   * Test search by term matching email.
   * Requirement 2.2: Search term should match email field
   */
  @Test
  @DisplayName("Should search leads by term matching email")
  @WithMockUser(authorities = {"USER"})
  void testSearchByTermMatchingEmail() throws Exception {
    // Given - Create leads with different emails
    Lead lead1 = new Lead("Alice", "Williams", "alice@techcorp.com", "Website");
    lead1.setCreatedBy("test-user");
    lead1.setUpdatedBy("test-user");
    leadRepository.save(lead1);

    Lead lead2 = new Lead("Bob", "Brown", "bob@example.com", "Referral");
    lead2.setCreatedBy("test-user");
    lead2.setUpdatedBy("test-user");
    leadRepository.save(lead2);

    // When & Then - Search for "techcorp" should match alice@techcorp.com
    mockMvc.perform(get("/api/v1/leads")
            .param("search", "techcorp")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()", is(1)))
        .andExpect(jsonPath("$.content[0].email", is("alice@techcorp.com")));
  }

  /**
   * Test search by term matching company.
   * Requirement 2.2: Search term should match company field
   */
  @Test
  @DisplayName("Should search leads by term matching company")
  @WithMockUser(authorities = {"USER"})
  void testSearchByTermMatchingCompany() throws Exception {
    // Given - Create leads with different companies
    Lead lead1 = new Lead("Charlie", "Davis", "charlie@example.com", "Website");
    lead1.setCompany("Acme Corporation");
    lead1.setCreatedBy("test-user");
    lead1.setUpdatedBy("test-user");
    leadRepository.save(lead1);

    Lead lead2 = new Lead("Diana", "Evans", "diana@example.com", "Referral");
    lead2.setCompany("Tech Solutions");
    lead2.setCreatedBy("test-user");
    lead2.setUpdatedBy("test-user");
    leadRepository.save(lead2);

    // When & Then - Search for "Acme" should match "Acme Corporation"
    mockMvc.perform(get("/api/v1/leads")
            .param("search", "Acme")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()", is(1)))
        .andExpect(jsonPath("$.content[0].company", is("Acme Corporation")));
  }

  /**
   * Test search by term matching phone.
   * Requirement 2.2: Search term should match phone field
   */
  @Test
  @DisplayName("Should search leads by term matching phone")
  @WithMockUser(authorities = {"USER"})
  void testSearchByTermMatchingPhone() throws Exception {
    // Given - Create leads with different phone numbers
    Lead lead1 = new Lead("Eve", "Foster", "eve@example.com", "Website");
    lead1.setPhone("+1234567890");
    lead1.setCreatedBy("test-user");
    lead1.setUpdatedBy("test-user");
    leadRepository.save(lead1);

    Lead lead2 = new Lead("Frank", "Green", "frank@example.com", "Referral");
    lead2.setPhone("+9876543210");
    lead2.setCreatedBy("test-user");
    lead2.setUpdatedBy("test-user");
    leadRepository.save(lead2);

    // When & Then - Search for "123456" should match "+1234567890"
    mockMvc.perform(get("/api/v1/leads")
            .param("search", "123456")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()", is(1)))
        .andExpect(jsonPath("$.content[0].phone", is("+1234567890")));
  }

  /**
   * Test filter by source.
   * Requirement 2.3: WHEN a user filters by lead source,
   * THE CRM_System SHALL return only leads from that source
   */
  @Test
  @DisplayName("Should filter leads by source")
  @WithMockUser(authorities = {"USER"})
  void testFilterBySource() throws Exception {
    // Given - Create leads with different sources
    Lead lead1 = new Lead("George", "Harris", "george@example.com", "Website");
    lead1.setCreatedBy("test-user");
    lead1.setUpdatedBy("test-user");
    leadRepository.save(lead1);

    Lead lead2 = new Lead("Helen", "Irving", "helen@example.com", "Website");
    lead2.setCreatedBy("test-user");
    lead2.setUpdatedBy("test-user");
    leadRepository.save(lead2);

    Lead lead3 = new Lead("Ian", "Jones", "ian@example.com", "Referral");
    lead3.setCreatedBy("test-user");
    lead3.setUpdatedBy("test-user");
    leadRepository.save(lead3);

    Lead lead4 = new Lead("Julia", "King", "julia@example.com", "Cold Call");
    lead4.setCreatedBy("test-user");
    lead4.setUpdatedBy("test-user");
    leadRepository.save(lead4);

    // When & Then - Filter by "Website" source
    mockMvc.perform(get("/api/v1/leads")
            .param("source", "Website")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()", is(2)))
        .andExpect(jsonPath("$.totalElements", is(2)))
        .andExpect(jsonPath("$.content[0].source", is("Website")))
        .andExpect(jsonPath("$.content[1].source", is("Website")));
  }

  /**
   * Test filter by status.
   * Requirement 2.4: WHEN a user filters by pipeline stage (status),
   * THE CRM_System SHALL return only leads in that stage
   */
  @Test
  @DisplayName("Should filter leads by status")
  @WithMockUser(authorities = {"USER"})
  void testFilterByStatus() throws Exception {
    // Given - Create leads with different statuses
    Lead lead1 = new Lead("Kevin", "Lee", "kevin@example.com", "Website");
    lead1.setStatus(LeadStatus.NEW);
    lead1.setCreatedBy("test-user");
    lead1.setUpdatedBy("test-user");
    leadRepository.save(lead1);

    Lead lead2 = new Lead("Laura", "Miller", "laura@example.com", "Referral");
    lead2.setStatus(LeadStatus.CONTACTED);
    lead2.setCreatedBy("test-user");
    lead2.setUpdatedBy("test-user");
    leadRepository.save(lead2);

    Lead lead3 = new Lead("Mike", "Nelson", "mike@example.com", "Cold Call");
    lead3.setStatus(LeadStatus.QUALIFIED);
    lead3.setCreatedBy("test-user");
    lead3.setUpdatedBy("test-user");
    leadRepository.save(lead3);

    // When & Then - Filter by "CONTACTED" status
    mockMvc.perform(get("/api/v1/leads")
            .param("status", "CONTACTED")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()", is(1)))
        .andExpect(jsonPath("$.totalElements", is(1)))
        .andExpect(jsonPath("$.content[0].status", is("CONTACTED")));
  }

  /**
   * Test filter by assigned user.
   * Requirement 2.5: WHEN a user filters by assigned user,
   * THE CRM_System SHALL return only leads assigned to that user
   */
  @Test
  @DisplayName("Should filter leads by assigned user")
  @WithMockUser(authorities = {"USER"})
  void testFilterByAssignedUser() throws Exception {
    // Given - Create leads with different assigned users
    Lead lead1 = new Lead("Nancy", "Owen", "nancy@example.com", "Website");
    lead1.setAssignedTo("user1");
    lead1.setCreatedBy("test-user");
    lead1.setUpdatedBy("test-user");
    leadRepository.save(lead1);

    Lead lead2 = new Lead("Oscar", "Parker", "oscar@example.com", "Referral");
    lead2.setAssignedTo("user2");
    lead2.setCreatedBy("test-user");
    lead2.setUpdatedBy("test-user");
    leadRepository.save(lead2);

    Lead lead3 = new Lead("Paula", "Quinn", "paula@example.com", "Cold Call");
    lead3.setAssignedTo("user1");
    lead3.setCreatedBy("test-user");
    lead3.setUpdatedBy("test-user");
    leadRepository.save(lead3);

    // When & Then - Filter by "user1"
    mockMvc.perform(get("/api/v1/leads")
            .param("assignedTo", "user1")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()", is(2)))
        .andExpect(jsonPath("$.totalElements", is(2)))
        .andExpect(jsonPath("$.content[0].assignedTo", is("user1")))
        .andExpect(jsonPath("$.content[1].assignedTo", is("user1")));
  }

  /**
   * Test combined filters.
   * Requirement 2.6: WHEN a user combines multiple filters,
   * THE CRM_System SHALL apply all filters with AND logic
   */
  @Test
  @DisplayName("Should apply combined filters with AND logic")
  @WithMockUser(authorities = {"USER"})
  void testCombinedFilters() throws Exception {
    // Given - Create leads with various attributes
    Lead lead1 = new Lead("Rachel", "Roberts", "rachel@example.com", "Website");
    lead1.setStatus(LeadStatus.NEW);
    lead1.setAssignedTo("user1");
    lead1.setCreatedBy("test-user");
    lead1.setUpdatedBy("test-user");
    leadRepository.save(lead1);

    Lead lead2 = new Lead("Sam", "Scott", "sam@example.com", "Website");
    lead2.setStatus(LeadStatus.CONTACTED);
    lead2.setAssignedTo("user1");
    lead2.setCreatedBy("test-user");
    lead2.setUpdatedBy("test-user");
    leadRepository.save(lead2);

    Lead lead3 = new Lead("Tina", "Taylor", "tina@example.com", "Referral");
    lead3.setStatus(LeadStatus.NEW);
    lead3.setAssignedTo("user1");
    lead3.setCreatedBy("test-user");
    lead3.setUpdatedBy("test-user");
    leadRepository.save(lead3);

    Lead lead4 = new Lead("Uma", "Underwood", "uma@example.com", "Website");
    lead4.setStatus(LeadStatus.NEW);
    lead4.setAssignedTo("user2");
    lead4.setCreatedBy("test-user");
    lead4.setUpdatedBy("test-user");
    leadRepository.save(lead4);

    // When & Then - Filter by source=Website AND status=NEW AND assignedTo=user1
    mockMvc.perform(get("/api/v1/leads")
            .param("source", "Website")
            .param("status", "NEW")
            .param("assignedTo", "user1")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()", is(1)))
        .andExpect(jsonPath("$.totalElements", is(1)))
        .andExpect(jsonPath("$.content[0].firstName", is("Rachel")))
        .andExpect(jsonPath("$.content[0].source", is("Website")))
        .andExpect(jsonPath("$.content[0].status", is("NEW")))
        .andExpect(jsonPath("$.content[0].assignedTo", is("user1")));
  }

  /**
   * Test combined search and filters.
   * Requirement 2.6: Search term combined with filters should apply AND logic
   */
  @Test
  @DisplayName("Should apply search term with filters using AND logic")
  @WithMockUser(authorities = {"USER"})
  void testSearchWithFilters() throws Exception {
    // Given - Create leads with various attributes
    Lead lead1 = new Lead("Victor", "Smith", "victor@example.com", "Website");
    lead1.setStatus(LeadStatus.NEW);
    lead1.setCreatedBy("test-user");
    lead1.setUpdatedBy("test-user");
    leadRepository.save(lead1);

    Lead lead2 = new Lead("Wendy", "Smith", "wendy@example.com", "Referral");
    lead2.setStatus(LeadStatus.NEW);
    lead2.setCreatedBy("test-user");
    lead2.setUpdatedBy("test-user");
    leadRepository.save(lead2);

    Lead lead3 = new Lead("Xavier", "Smith", "xavier@example.com", "Website");
    lead3.setStatus(LeadStatus.CONTACTED);
    lead3.setCreatedBy("test-user");
    lead3.setUpdatedBy("test-user");
    leadRepository.save(lead3);

    // When & Then - Search for "Smith" AND filter by source=Website AND status=NEW
    mockMvc.perform(get("/api/v1/leads")
            .param("search", "Smith")
            .param("source", "Website")
            .param("status", "NEW")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()", is(1)))
        .andExpect(jsonPath("$.totalElements", is(1)))
        .andExpect(jsonPath("$.content[0].firstName", is("Victor")))
        .andExpect(jsonPath("$.content[0].lastName", is("Smith")))
        .andExpect(jsonPath("$.content[0].source", is("Website")))
        .andExpect(jsonPath("$.content[0].status", is("NEW")));
  }
}
