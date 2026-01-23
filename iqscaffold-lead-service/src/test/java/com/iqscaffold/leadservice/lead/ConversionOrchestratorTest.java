package com.iqscaffold.leadservice.lead;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import com.iqscaffold.leadservice.event.LeadEventPublisher;
import com.iqscaffold.leadservice.infrastructure.client.ContactServiceClient;
import com.iqscaffold.leadservice.infrastructure.client.PipelineServiceClient;
import com.iqscaffold.leadservice.lead.dto.LeadDtos;
import com.iqscaffold.leadservice.shared.exception.LeadNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConversionOrchestrator Tests")
class ConversionOrchestratorTest {

  @Mock
  private LeadRepository leadRepository;

  @Mock
  private ContactServiceClient contactServiceClient;

  @Mock
  private PipelineServiceClient pipelineServiceClient;

  @Mock
  private LeadEventPublisher leadEventPublisher;

  private ConversionOrchestrator conversionOrchestrator;

  @BeforeEach
  void setUp() {
    conversionOrchestrator = new ConversionOrchestrator(
        leadRepository,
        contactServiceClient,
        pipelineServiceClient,
        leadEventPublisher
    );
  }

  @Test
  @DisplayName("Should successfully convert lead to contact and create pipeline item")
  void shouldSuccessfullyConvertLead() {
    // Given
    Long leadId = 1L;
    Lead lead = createTestLead(leadId);
    LeadDtos.ConvertLeadRequest request = new LeadDtos.ConvertLeadRequest(null, "Test notes");
    String bearerToken = "test-token";
    String convertedBy = "test-user";

    ContactServiceClient.ContactResponse contactResponse =
        new ContactServiceClient.ContactResponse(
            100L, "John", "Doe", "john.doe@example.com", "123-456-7890",
            "Developer", null, "CUSTOMER", 85, "Test notes",
            leadId, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(),
            "system", "system"
        );

    PipelineServiceClient.PipelineItemResponse pipelineResponse =
        new PipelineServiceClient.PipelineItemResponse(
            200L, leadId, 1L, BigDecimal.valueOf(15000), BigDecimal.valueOf(85),
            LocalDateTime.now(), 0, null, LocalDateTime.now(), LocalDateTime.now(),
            "system", "system"
        );

    when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
    when(contactServiceClient.createContact(any(), anyString())).thenReturn(contactResponse);
    when(pipelineServiceClient.createPipelineItem(any(), anyString())).thenReturn(pipelineResponse);
    when(leadRepository.save(any(Lead.class))).thenReturn(lead);

    // When
    LeadDtos.ConvertLeadResponse result = conversionOrchestrator.convertLead(
        leadId, request, bearerToken, convertedBy);

    // Then
    assertNotNull(result);
    assertEquals(leadId, result.leadId());
    assertEquals(contactResponse.id(), result.contactId());
    assertNotNull(result.convertedAt());

    verify(leadRepository).findById(leadId);
    verify(contactServiceClient).createContact(any(), anyString());
    verify(pipelineServiceClient).createPipelineItem(any(), anyString());
    verify(leadRepository).save(any(Lead.class));
    verify(leadEventPublisher).publishLeadConverted(any(Lead.class), any(Long.class));
  }

  @Test
  @DisplayName("Should throw exception when lead not found")
  void shouldThrowExceptionWhenLeadNotFound() {
    // Given
    Long leadId = 999L;
    LeadDtos.ConvertLeadRequest request = new LeadDtos.ConvertLeadRequest(null, "Test notes");
    String bearerToken = "test-token";
    String convertedBy = "test-user";

    when(leadRepository.findById(leadId)).thenReturn(Optional.empty());

    // When & Then
    assertThrows(LeadNotFoundException.class, () ->
        conversionOrchestrator.convertLead(leadId, request, bearerToken, convertedBy));

    verify(leadRepository).findById(leadId);
  }

  @Test
  @DisplayName("Should throw exception when lead already converted")
  void shouldThrowExceptionWhenLeadAlreadyConverted() {
    // Given
    Long leadId = 1L;
    Lead lead = createTestLead(leadId);
    lead.setStatus(LeadStatus.CONVERTED);
    lead.setConvertedToContactId(100L);

    LeadDtos.ConvertLeadRequest request = new LeadDtos.ConvertLeadRequest(null, "Test notes");
    String bearerToken = "test-token";
    String convertedBy = "test-user";

    when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));

    // When & Then
    assertThrows(IllegalStateException.class, () ->
        conversionOrchestrator.convertLead(leadId, request, bearerToken, convertedBy));

    verify(leadRepository).findById(leadId);
  }

  private Lead createTestLead(Long id) {
    Lead lead = new Lead();
    lead.setId(id);
    lead.setFirstName("John");
    lead.setLastName("Doe");
    lead.setEmail("john.doe@example.com");
    lead.setPhone("123-456-7890");
    lead.setCompany("Test Company");
    lead.setJobTitle("Developer");
    lead.setSource("Website");
    lead.setStatus(LeadStatus.QUALIFIED);
    lead.setScore(85);
    lead.setQualified(true);
    lead.setNotes("Test lead notes");
    lead.setCreatedBy("system");
    lead.setUpdatedBy("system");
    lead.setCreatedAt(LocalDateTime.now());
    lead.setUpdatedAt(LocalDateTime.now());
    return lead;
  }
}
