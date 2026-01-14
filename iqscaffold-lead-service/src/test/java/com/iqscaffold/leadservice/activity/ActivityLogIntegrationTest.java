package com.iqscaffold.leadservice.activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqscaffold.leadservice.lead.Lead;
import com.iqscaffold.leadservice.lead.LeadRepository;
import com.iqscaffold.leadservice.note.LeadNote;
import com.iqscaffold.leadservice.note.LeadNoteRepository;
import com.iqscaffold.leadservice.note.dto.LeadNoteDtos;
import java.util.List;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Activity Logging.
 * Tests Requirements: 7.1, 7.2, 7.3, 7.6
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ActivityLogIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private LeadRepository leadRepository;

  @Autowired
  private LeadNoteRepository leadNoteRepository;

  @Autowired
  private LeadActivityRepository activityRepository;

  @Autowired
  private ActivityLogService activityLogService;

  @BeforeEach
  void setUp() {
    // Clean up in reverse order of dependencies
    // Note: @Transactional on class level will roll back changes after each test
    leadNoteRepository.deleteAll();
    leadRepository.deleteAll();
    // Don't delete activities here as they have foreign key constraints
  }

  /**
   * Test lead created activity logged.
   * Requirement 7.1: WHEN a lead is created, THE CRM_System SHALL log a "lead.created" activity
   * with the creation details
   */
  @Test
  @DisplayName("Should log LEAD_CREATED activity when lead is created")
  @WithMockUser(authorities = {"USER"})
  void testLeadCreatedActivityLogged() {
    // Given - Create a lead
    Lead lead = new Lead("John", "Doe", "john.doe@example.com", "Website");
    lead.setPhone("+1234567890");
    lead.setCompany("Acme Corp");
    lead.setCreatedBy("test-user");
    lead.setUpdatedBy("test-user");
    Lead savedLead = leadRepository.save(lead);

    // When - Log the lead created activity
    activityLogService.logLeadCreated(savedLead);

    // Then - Verify activity was logged
    List<LeadActivity> activities = activityRepository.findByLeadIdOrderByCreatedAtDesc(
        savedLead.getId());
    assertThat(activities).hasSize(1);

    LeadActivity activity = activities.get(0);
    assertThat(activity.getType()).isEqualTo(ActivityType.LEAD_CREATED);
    assertThat(activity.getDescription()).contains("Lead created");
    assertThat(activity.getDescription()).contains("John Doe");
    assertThat(activity.getDescription()).contains("john.doe@example.com");
    assertThat(activity.getDescription()).contains("Website");
    assertThat(activity.getCreatedBy()).isEqualTo("test-user");
    assertThat(activity.getCreatedAt()).isNotNull();
    assertThat(activity.getMetadata()).isNotNull();
  }

  /**
   * Test note added activity logged.
   * Requirement 7.3: WHEN a note is added to a lead, THE CRM_System SHALL log a "note.added"
   * activity
   */
  @Test
  @DisplayName("Should log NOTE_ADDED activity when note is added to lead")
  @WithMockUser(authorities = {"USER"})
  void testNoteAddedActivityLogged() throws Exception {
    // Given - Create a lead first
    Lead lead = new Lead("Jane", "Smith", "jane.smith@example.com", "Referral");
    lead.setCreatedBy("test-user");
    lead.setUpdatedBy("test-user");
    Lead savedLead = leadRepository.save(lead);

    // When - Add a note to the lead via REST API
    LeadNoteDtos.CreateLeadNoteRequest noteRequest = new LeadNoteDtos.CreateLeadNoteRequest(
        "This is an important note about the lead",
        false
    );

    mockMvc.perform(post("/api/v1/leads/{leadId}/notes", savedLead.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(noteRequest)))
        .andExpect(status().isCreated());

    // Then - Verify NOTE_ADDED activity was logged
    List<LeadActivity> activities = activityRepository.findByLeadIdAndType(
        savedLead.getId(), ActivityType.NOTE_ADDED);
    assertThat(activities).hasSize(1);

    LeadActivity activity = activities.get(0);
    assertThat(activity.getType()).isEqualTo(ActivityType.NOTE_ADDED);
    assertThat(activity.getDescription()).contains("Note added");
    assertThat(activity.getDescription()).contains("Jane Smith");
    assertThat(activity.getCreatedBy()).isNotNull();
    assertThat(activity.getCreatedAt()).isNotNull();
    assertThat(activity.getMetadata()).isNotNull();
    assertThat(activity.getMetadata()).contains("noteId");
    assertThat(activity.getMetadata()).contains("contentPreview");
  }

  /**
   * Test activity timeline retrieval.
   * Requirement 7.6: WHEN a user requests a lead's activity log, THE CRM_System SHALL return
   * all activities sorted by timestamp descending
   */
  @Test
  @DisplayName("Should retrieve activity timeline sorted by timestamp descending")
  @WithMockUser(authorities = {"USER"})
  void testActivityTimelineRetrieval() throws Exception {
    // Given - Create a lead
    Lead lead = new Lead("Bob", "Johnson", "bob.johnson@example.com", "Cold Call");
    lead.setCreatedBy("test-user");
    lead.setUpdatedBy("test-user");
    Lead savedLead = leadRepository.save(lead);

    // Log lead created activity
    activityLogService.logLeadCreated(savedLead);

    // Add a note (which will log NOTE_ADDED activity)
    LeadNote note = new LeadNote(savedLead, "First note about the lead", "test-user");
    LeadNote savedNote = leadNoteRepository.save(note);
    activityLogService.logNoteAdded(savedLead, savedNote);

    // Update the lead (which will log LEAD_UPDATED activity)
    savedLead.setPhone("+9876543210");
    savedLead.setUpdatedBy("test-user");
    leadRepository.save(savedLead);
    activityLogService.logLeadUpdated(savedLead);

    // When - Request activity timeline via REST API
    mockMvc.perform(get("/api/v1/leads/{leadId}/activities", savedLead.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        // Verify activities are sorted by timestamp descending (newest first)
        .andExpect(jsonPath("$[0].type", is("LEAD_UPDATED")))
        .andExpect(jsonPath("$[0].description", notNullValue()))
        .andExpect(jsonPath("$[0].createdAt", notNullValue()))
        .andExpect(jsonPath("$[0].createdBy", notNullValue()))
        .andExpect(jsonPath("$[1].type", is("NOTE_ADDED")))
        .andExpect(jsonPath("$[1].description", notNullValue()))
        .andExpect(jsonPath("$[1].createdAt", notNullValue()))
        .andExpect(jsonPath("$[2].type", is("LEAD_CREATED")))
        .andExpect(jsonPath("$[2].description", notNullValue()))
        .andExpect(jsonPath("$[2].createdAt", notNullValue()));

    // Then - Verify activities are in correct order
    List<LeadActivity> activities = activityRepository.findByLeadIdOrderByCreatedAtDesc(
        savedLead.getId());
    assertThat(activities).hasSize(3);
    assertThat(activities.get(0).getType()).isEqualTo(ActivityType.LEAD_UPDATED);
    assertThat(activities.get(1).getType()).isEqualTo(ActivityType.NOTE_ADDED);
    assertThat(activities.get(2).getType()).isEqualTo(ActivityType.LEAD_CREATED);
  }

  /**
   * Test activity timeline includes all required metadata.
   * Requirement 7.7: WHEN an activity is logged, THE CRM_System SHALL include the user ID,
   * timestamp, activity type, and relevant metadata
   */
  @Test
  @DisplayName("Should include all required metadata in activity log")
  @WithMockUser(authorities = {"USER"})
  void testActivityIncludesRequiredMetadata() {
    // Given - Create a lead
    Lead lead = new Lead("Alice", "Williams", "alice.williams@example.com", "Email Campaign");
    lead.setCreatedBy("test-user-123");
    lead.setUpdatedBy("test-user-123");
    Lead savedLead = leadRepository.save(lead);

    // When - Log the lead created activity
    activityLogService.logLeadCreated(savedLead);

    // Then - Verify all required metadata is present
    List<LeadActivity> activities = activityRepository.findByLeadIdOrderByCreatedAtDesc(
        savedLead.getId());
    assertThat(activities).hasSize(1);

    LeadActivity activity = activities.get(0);
    // Verify user ID (createdBy)
    assertThat(activity.getCreatedBy()).isEqualTo("test-user-123");

    // Verify timestamp
    assertThat(activity.getCreatedAt()).isNotNull();

    // Verify activity type
    assertThat(activity.getType()).isEqualTo(ActivityType.LEAD_CREATED);

    // Verify relevant metadata
    assertThat(activity.getMetadata()).isNotNull();
    assertThat(activity.getMetadata()).contains("leadId");
    assertThat(activity.getMetadata()).contains("email");
    assertThat(activity.getMetadata()).contains("source");
    assertThat(activity.getMetadata()).contains("status");
  }

  /**
   * Test activity timeline for non-existent lead returns 404.
   * Requirement 7.6: Should return 404 when requesting activities for non-existent lead
   */
  @Test
  @DisplayName("Should return 404 when requesting activities for non-existent lead")
  @WithMockUser(authorities = {"USER"})
  void testActivityTimelineForNonExistentLead() throws Exception {
    // When & Then - Request activity timeline for non-existent lead
    mockMvc.perform(get("/api/v1/leads/{leadId}/activities", 99999L)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  /**
   * Test multiple activities are logged in correct order.
   * Requirement 7.6: Activities should be sorted by timestamp descending
   */
  @Test
  @DisplayName("Should log multiple activities in correct chronological order")
  @WithMockUser(authorities = {"USER"})
  void testMultipleActivitiesLoggedInOrder() {
    // Given - Create a lead
    Lead lead = new Lead("Charlie", "Davis", "charlie.davis@example.com", "Trade Show");
    lead.setCreatedBy("test-user");
    lead.setUpdatedBy("test-user");
    Lead savedLead = leadRepository.save(lead);

    // When - Log multiple activities
    activityLogService.logLeadCreated(savedLead);

    // Add first note
    LeadNote note1 = new LeadNote(savedLead, "First note", "test-user");
    LeadNote savedNote1 = leadNoteRepository.save(note1);
    activityLogService.logNoteAdded(savedLead, savedNote1);

    // Add second note
    LeadNote note2 = new LeadNote(savedLead, "Second note", "test-user");
    LeadNote savedNote2 = leadNoteRepository.save(note2);
    activityLogService.logNoteAdded(savedLead, savedNote2);

    // Update lead
    savedLead.setCompany("New Company");
    savedLead.setUpdatedBy("test-user");
    leadRepository.save(savedLead);
    activityLogService.logLeadUpdated(savedLead);

    // Then - Verify activities are in correct order (newest first)
    List<LeadActivity> activities = activityRepository.findByLeadIdOrderByCreatedAtDesc(
        savedLead.getId());
    assertThat(activities).hasSize(4);
    assertThat(activities.get(0).getType()).isEqualTo(ActivityType.LEAD_UPDATED);
    assertThat(activities.get(1).getType()).isEqualTo(ActivityType.NOTE_ADDED);
    assertThat(activities.get(2).getType()).isEqualTo(ActivityType.NOTE_ADDED);
    assertThat(activities.get(3).getType()).isEqualTo(ActivityType.LEAD_CREATED);

    // Verify timestamps are in descending order
    for (int i = 0; i < activities.size() - 1; i++) {
      assertThat(activities.get(i).getCreatedAt())
          .isAfterOrEqualTo(activities.get(i + 1).getCreatedAt());
    }
  }
}
