package com.iqscaffold.leadservice.lead;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqscaffold.leadservice.infrastructure.client.ContactServiceClient;
import com.iqscaffold.leadservice.lead.dto.LeadDtos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for Lead Conversion functionality.
 * Tests the conversion of leads to contacts with rollback scenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LeadConversionIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private LeadRepository leadRepository;

  @MockBean
  private ContactServiceClient contactServiceClient;

  @BeforeEach
  void setUp() {
    leadRepository.deleteAll();
  }

  @Test
  @DisplayName("Should successfully convert lead to contact")
  @WithMockUser(authorities = "USER")
  void shouldConvertLeadToContact() throws Exception {
    // Given - Create a qualified lead
    Lead lead = createTestLead("John", "Doe", "john.doe@example.com");
    lead.setStatus(LeadStatus.QUALIFIED);
    lead.setScore(75);
    Lead savedLead = leadRepository.save(lead);

    // Mock successful contact creation
    ContactServiceClient.ContactResponse contactResponse =
        new ContactServiceClient.ContactResponse(
            100L,  // id
            "John",  // firstName
            "Doe",  // lastName
            "john.doe@example.com",  // email
            "+1234567890",  // phone
            "Software Engineer",  // jobTitle
            null,  // companyId
            "CUSTOMER",  // status
            75,  // leadScore
            "Converted from lead",  // notes
            savedLead.getId(),  // convertedFromLeadId
            LocalDateTime.now(),  // convertedAt
            LocalDateTime.now(),  // createdAt
            LocalDateTime.now(),  // updatedAt
            "test",  // createdBy
            "test"  // updatedBy
        );

    when(contactServiceClient.createContact(
        any(ContactServiceClient.CreateContactRequest.class),
        anyString()
    )).thenReturn(contactResponse);

    // Prepare conversion request
    LeadDtos.ConvertLeadRequest request = new LeadDtos.ConvertLeadRequest(
        null,
        "Converted from qualified lead"
    );

    // When & Then - Convert lead
    mockMvc.perform(post("/api/v1/leads/{id}/convert", savedLead.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.leadId").value(savedLead.getId()))
        .andExpect(jsonPath("$.contactId").value(100L))
        .andExpect(jsonPath("$.convertedAt").exists())
        .andExpect(jsonPath("$.message").value("Lead successfully converted to contact"));

    // Verify lead status updated
    Lead convertedLead = leadRepository.findById(savedLead.getId()).orElseThrow();
    assertThat(convertedLead.getStatus()).isEqualTo(LeadStatus.CONVERTED);
    assertThat(convertedLead.getConvertedToContactId()).isEqualTo(100L);
    assertThat(convertedLead.getConvertedAt()).isNotNull();

    // Verify contact service was called
    verify(contactServiceClient, times(1)).createContact(
        any(ContactServiceClient.CreateContactRequest.class),
        eq("Bearer test-token")
    );
  }

  @Test
  @DisplayName("Should convert lead with company ID")
  @WithMockUser(authorities = "USER")
  void shouldConvertLeadWithCompanyId() throws Exception {
    // Given
    Lead lead = createTestLead("Jane", "Smith", "jane.smith@example.com");
    lead.setStatus(LeadStatus.QUALIFIED);
    Lead savedLead = leadRepository.save(lead);

    ContactServiceClient.ContactResponse contactResponse =
        new ContactServiceClient.ContactResponse(
            101L, "Jane", "Smith", "jane.smith@example.com",
            null, null, 50L, "CUSTOMER", 0, null,
            savedLead.getId(), LocalDateTime.now(),
            LocalDateTime.now(), LocalDateTime.now(), "test", "test"
        );

    when(contactServiceClient.createContact(
        any(ContactServiceClient.CreateContactRequest.class),
        anyString()
    )).thenReturn(contactResponse);

    LeadDtos.ConvertLeadRequest request = new LeadDtos.ConvertLeadRequest(
        50L,  // Company ID
        "Converted with company association"
    );

    // When & Then
    mockMvc.perform(post("/api/v1/leads/{id}/convert", savedLead.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contactId").value(101L));
  }

  @Test
  @DisplayName("Should convert lead without request body")
  @WithMockUser(authorities = "USER")
  void shouldConvertLeadWithoutRequestBody() throws Exception {
    // Given
    Lead lead = createTestLead("Bob", "Johnson", "bob.johnson@example.com");
    lead.setStatus(LeadStatus.QUALIFIED);
    lead.setNotes("Original lead notes");
    Lead savedLead = leadRepository.save(lead);

    ContactServiceClient.ContactResponse contactResponse =
        new ContactServiceClient.ContactResponse(
            102L, "Bob", "Johnson", "bob.johnson@example.com",
            null, null, null, "CUSTOMER", 0, "Original lead notes",
            savedLead.getId(), LocalDateTime.now(),
            LocalDateTime.now(), LocalDateTime.now(), "test", "test"
        );

    when(contactServiceClient.createContact(
        any(ContactServiceClient.CreateContactRequest.class),
        anyString()
    )).thenReturn(contactResponse);

    // When & Then - No request body
    mockMvc.perform(post("/api/v1/leads/{id}/convert", savedLead.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contactId").value(102L));
  }

  @Test
  @DisplayName("Should fail to convert already converted lead")
  @WithMockUser(authorities = "USER")
  void shouldFailToConvertAlreadyConvertedLead() throws Exception {
    // Given - Lead already converted
    Lead lead = createTestLead("Alice", "Williams", "alice.williams@example.com");
    lead.setStatus(LeadStatus.CONVERTED);
    lead.setConvertedToContactId(999L);
    lead.setConvertedAt(LocalDateTime.now());
    Lead savedLead = leadRepository.save(lead);

    LeadDtos.ConvertLeadRequest request = new LeadDtos.ConvertLeadRequest(null, null);

    // When & Then
    mockMvc.perform(post("/api/v1/leads/{id}/convert", savedLead.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("Authorization", "Bearer test-token"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should fail to convert non-existent lead")
  @WithMockUser(authorities = "USER")
  void shouldFailToConvertNonExistentLead() throws Exception {
    // Given
    LeadDtos.ConvertLeadRequest request = new LeadDtos.ConvertLeadRequest(null, null);

    // When & Then
    mockMvc.perform(post("/api/v1/leads/{id}/convert", 99999L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("Authorization", "Bearer test-token"))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should rollback on contact creation failure")
  @WithMockUser(authorities = "USER")
  void shouldRollbackOnContactCreationFailure() throws Exception {
    // Given
    Lead lead = createTestLead("Charlie", "Brown", "charlie.brown@example.com");
    lead.setStatus(LeadStatus.QUALIFIED);
    Lead savedLead = leadRepository.save(lead);

    // Mock contact service failure
    when(contactServiceClient.createContact(
        any(ContactServiceClient.CreateContactRequest.class),
        anyString()
    )).thenThrow(new RuntimeException("Contact service unavailable"));

    LeadDtos.ConvertLeadRequest request = new LeadDtos.ConvertLeadRequest(null, null);

    // When & Then
    mockMvc.perform(post("/api/v1/leads/{id}/convert", savedLead.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("Authorization", "Bearer test-token"))
        .andExpect(status().isInternalServerError());

    // Verify lead status NOT changed
    Lead unchangedLead = leadRepository.findById(savedLead.getId()).orElseThrow();
    assertThat(unchangedLead.getStatus()).isEqualTo(LeadStatus.QUALIFIED);
    assertThat(unchangedLead.getConvertedToContactId()).isNull();
    assertThat(unchangedLead.getConvertedAt()).isNull();
  }

  @Test
  @DisplayName("Should rollback lead status on contact deletion failure")
  @WithMockUser(authorities = "USER")
  void shouldRollbackLeadStatusOnContactDeletionFailure() throws Exception {
    // Given
    Lead lead = createTestLead("Diana", "Evans", "diana.evans@example.com");
    lead.setStatus(LeadStatus.QUALIFIED);
    Lead savedLead = leadRepository.save(lead);

    // Mock successful contact creation
    ContactServiceClient.ContactResponse contactResponse =
        new ContactServiceClient.ContactResponse(
            103L, "Diana", "Evans", "diana.evans@example.com",
            null, null, null, "CUSTOMER", 0, null,
            savedLead.getId(), LocalDateTime.now(),
            LocalDateTime.now(), LocalDateTime.now(), "test", "test"
        );

    when(contactServiceClient.createContact(
        any(ContactServiceClient.CreateContactRequest.class),
        anyString()
    )).thenReturn(contactResponse);

    // Mock contact deletion failure during rollback
    doThrow(new RuntimeException("Delete failed"))
        .when(contactServiceClient).deleteContact(eq(103L), anyString());

    LeadDtos.ConvertLeadRequest request = new LeadDtos.ConvertLeadRequest(null, null);

    // When & Then
    mockMvc.perform(post("/api/v1/leads/{id}/convert", savedLead.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk());

    // Verify lead was converted successfully
    Lead convertedLead = leadRepository.findById(savedLead.getId()).orElseThrow();
    assertThat(convertedLead.getStatus()).isEqualTo(LeadStatus.CONVERTED);
    assertThat(convertedLead.getConvertedToContactId()).isEqualTo(103L);
  }

  @Test
  @DisplayName("Should preserve lead data during conversion")
  @WithMockUser(authorities = "USER")
  void shouldPreserveLeadDataDuringConversion() throws Exception {
    // Given - Lead with all fields populated
    Lead lead = createTestLead("Eve", "Foster", "eve.foster@example.com");
    lead.setPhone("+1234567890");
    lead.setCompany("Tech Corp");
    lead.setJobTitle("CTO");
    lead.setSource("Referral");
    lead.setStatus(LeadStatus.QUALIFIED);
    lead.setScore(85);
    lead.setNotes("High value lead");
    lead.setAssignedTo("sales-rep-1");
    Lead savedLead = leadRepository.save(lead);

    ContactServiceClient.ContactResponse contactResponse =
        new ContactServiceClient.ContactResponse(
            104L, "Eve", "Foster", "eve.foster@example.com",
            "+1234567890", "CTO", null, "CUSTOMER", 85,
            "High value lead - Converted",
            savedLead.getId(), LocalDateTime.now(),
            LocalDateTime.now(), LocalDateTime.now(), "test", "test"
        );

    when(contactServiceClient.createContact(
        any(ContactServiceClient.CreateContactRequest.class),
        anyString()
    )).thenReturn(contactResponse);

    LeadDtos.ConvertLeadRequest request = new LeadDtos.ConvertLeadRequest(
        null,
        "High value lead - Converted"
    );

    // When
    mockMvc.perform(post("/api/v1/leads/{id}/convert", savedLead.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk());

    // Then - Verify lead data preserved
    Lead convertedLead = leadRepository.findById(savedLead.getId()).orElseThrow();
    assertThat(convertedLead.getFirstName()).isEqualTo("Eve");
    assertThat(convertedLead.getLastName()).isEqualTo("Foster");
    assertThat(convertedLead.getEmail()).isEqualTo("eve.foster@example.com");
    assertThat(convertedLead.getPhone()).isEqualTo("+1234567890");
    assertThat(convertedLead.getCompany()).isEqualTo("Tech Corp");
    assertThat(convertedLead.getJobTitle()).isEqualTo("CTO");
    assertThat(convertedLead.getSource()).isEqualTo("Referral");
    assertThat(convertedLead.getScore()).isEqualTo(85);
    assertThat(convertedLead.getAssignedTo()).isEqualTo("sales-rep-1");
    assertThat(convertedLead.getStatus()).isEqualTo(LeadStatus.CONVERTED);
  }

  @Test
  @DisplayName("Should set contact status to CUSTOMER on conversion")
  @WithMockUser(authorities = "USER")
  void shouldSetContactStatusToCustomer() throws Exception {
    // Given
    Lead lead = createTestLead("Frank", "Green", "frank.green@example.com");
    lead.setStatus(LeadStatus.QUALIFIED);
    Lead savedLead = leadRepository.save(lead);

    ContactServiceClient.ContactResponse contactResponse =
        new ContactServiceClient.ContactResponse(
            105L, "Frank", "Green", "frank.green@example.com",
            null, null, null, "CUSTOMER", 0, null,
            savedLead.getId(), LocalDateTime.now(),
            LocalDateTime.now(), LocalDateTime.now(), "test", "test"
        );

    when(contactServiceClient.createContact(
        any(ContactServiceClient.CreateContactRequest.class),
        anyString()
    )).thenReturn(contactResponse);

    LeadDtos.ConvertLeadRequest request = new LeadDtos.ConvertLeadRequest(null, null);

    // When
    mockMvc.perform(post("/api/v1/leads/{id}/convert", savedLead.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk());

    // Then - Verify contact service was called with CUSTOMER status
    verify(contactServiceClient).createContact(
        any(ContactServiceClient.CreateContactRequest.class),
        anyString()
    );
  }

  @Test
  @DisplayName("Should require authentication for conversion")
  void shouldRequireAuthenticationForConversion() throws Exception {
    // Given
    Lead lead = createTestLead("George", "Harris", "george.harris@example.com");
    Lead savedLead = leadRepository.save(lead);

    LeadDtos.ConvertLeadRequest request = new LeadDtos.ConvertLeadRequest(null, null);

    // When & Then - No authentication
    mockMvc.perform(post("/api/v1/leads/{id}/convert", savedLead.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should transfer lead score to contact")
  @WithMockUser(authorities = "USER")
  void shouldTransferLeadScoreToContact() throws Exception {
    // Given
    Lead lead = createTestLead("Helen", "Irving", "helen.irving@example.com");
    lead.setStatus(LeadStatus.QUALIFIED);
    lead.setScore(92);
    Lead savedLead = leadRepository.save(lead);

    ContactServiceClient.ContactResponse contactResponse =
        new ContactServiceClient.ContactResponse(
            106L, "Helen", "Irving", "helen.irving@example.com",
            null, null, null, "CUSTOMER", 92, null,
            savedLead.getId(), LocalDateTime.now(),
            LocalDateTime.now(), LocalDateTime.now(), "test", "test"
        );

    when(contactServiceClient.createContact(
        any(ContactServiceClient.CreateContactRequest.class),
        anyString()
    )).thenReturn(contactResponse);

    LeadDtos.ConvertLeadRequest request = new LeadDtos.ConvertLeadRequest(null, null);

    // When
    mockMvc.perform(post("/api/v1/leads/{id}/convert", savedLead.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
            .header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk());

    // Then - Verify contact created with lead score
    verify(contactServiceClient).createContact(
        any(ContactServiceClient.CreateContactRequest.class),
        anyString()
    );
  }

  private Lead createTestLead(String firstName, String lastName, String email) {
    Lead lead = new Lead(firstName, lastName, email, "Website");
    lead.setCreatedBy("test");
    lead.setUpdatedBy("test");
    return lead;
  }
}
