package com.iqscaffold.pipelineservice.followup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqscaffold.pipelineservice.followup.dto.FollowUpDtos;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Follow-Up management operations.
 * Tests Requirements: 5.1, 5.2, 5.3, 5.4, 5.7
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FollowUpRestResourceIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private FollowUpRepository followUpRepository;

  @MockBean
  private org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory;

  @BeforeEach
  void setUp() {
    // Clean up data before each test
    followUpRepository.deleteAll();
  }

  /**
   * Test schedule follow-up.
   * Requirement 5.1: WHEN a user schedules a follow-up with a lead ID, due date, and description,
   * THE CRM_System SHALL create the follow-up record
   */
  @Test
  @DisplayName("Should schedule a follow-up")
  @WithMockUser(authorities = {"USER"})
  void testScheduleFollowUp() throws Exception {
    // Given
    LocalDateTime dueDate = LocalDateTime.now().plusDays(2);
    FollowUpDtos.CreateFollowUpRequest request = new FollowUpDtos.CreateFollowUpRequest(
        1L,
        "Call to discuss proposal",
        "Follow up on the proposal sent last week",
        dueDate,
        FollowUpPriority.HIGH,
        "user123"
    );

    // When & Then
    mockMvc.perform(post("/api/v1/follow-ups")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.leadId", is(1)))
        .andExpect(jsonPath("$.title", is("Call to discuss proposal")))
        .andExpect(jsonPath("$.description", is("Follow up on the proposal sent last week")))
        .andExpect(jsonPath("$.dueDate", notNullValue()))
        .andExpect(jsonPath("$.priority", is("HIGH")))
        .andExpect(jsonPath("$.status", is("PENDING")))
        .andExpect(jsonPath("$.assignedTo", is("user123")))
        .andExpect(jsonPath("$.completedAt", nullValue()))
        .andExpect(jsonPath("$.createdAt", notNullValue()))
        .andExpect(jsonPath("$.createdBy", notNullValue()));
  }

  /**
   * Test schedule follow-up with minimal data.
   * Requirement 5.1: Should create follow-up with only required fields
   */
  @Test
  @DisplayName("Should schedule a follow-up with minimal data")
  @WithMockUser(authorities = {"USER"})
  void testScheduleFollowUpMinimal() throws Exception {
    // Given
    LocalDateTime dueDate = LocalDateTime.now().plusDays(1);
    FollowUpDtos.CreateFollowUpRequest request = new FollowUpDtos.CreateFollowUpRequest(
        2L,
        "Quick check-in",
        null,
        dueDate,
        null,
        null
    );

    // When & Then
    mockMvc.perform(post("/api/v1/follow-ups")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.leadId", is(2)))
        .andExpect(jsonPath("$.title", is("Quick check-in")))
        .andExpect(jsonPath("$.description", nullValue()))
        .andExpect(jsonPath("$.priority", is("MEDIUM"))) // Default priority
        .andExpect(jsonPath("$.status", is("PENDING")));
  }

  /**
   * Test schedule follow-up with past due date.
   * Requirement 5.7: WHEN a user creates a follow-up with a past due date,
   * THE CRM_System SHALL accept it and mark it as overdue
   */
  @Test
  @DisplayName("Should schedule a follow-up with past due date")
  @WithMockUser(authorities = {"USER"})
  void testScheduleFollowUpWithPastDueDate() throws Exception {
    // Given - Create follow-up with past due date
    LocalDateTime pastDueDate = LocalDateTime.now().minusDays(2);
    FollowUpDtos.CreateFollowUpRequest request = new FollowUpDtos.CreateFollowUpRequest(
        3L,
        "Overdue follow-up",
        "This should be marked as overdue",
        pastDueDate,
        FollowUpPriority.HIGH,
        null
    );

    // When & Then
    mockMvc.perform(post("/api/v1/follow-ups")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.leadId", is(3)))
        .andExpect(jsonPath("$.overdue", is(true)))
        .andExpect(jsonPath("$.status", is("PENDING")));
  }

  /**
   * Test schedule follow-up with invalid data.
   * Requirement 5.1: Should validate required fields
   */
  @Test
  @DisplayName("Should return 400 when scheduling follow-up with missing required fields")
  @WithMockUser(authorities = {"USER"})
  void testScheduleFollowUpInvalidData() throws Exception {
    // Given - Missing required fields
    FollowUpDtos.CreateFollowUpRequest request = new FollowUpDtos.CreateFollowUpRequest(
        null, // Missing leadId
        null, // Missing title
        "Description",
        null, // Missing dueDate
        null,
        null
    );

    // When & Then
    mockMvc.perform(post("/api/v1/follow-ups")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  /**
   * Test get today's follow-ups.
   * Requirement 5.2: WHEN a user requests today's follow-ups,
   * THE CRM_System SHALL return all follow-ups with due dates matching the current date
   */
  @Test
  @DisplayName("Should get today's follow-ups")
  @WithMockUser(authorities = {"USER"})
  void testGetTodaysFollowUps() throws Exception {
    // Given - Create follow-ups with different due dates
    LocalDate today = LocalDate.now();
    LocalDateTime todayMorning = today.atTime(9, 0);
    LocalDateTime todayAfternoon = today.atTime(14, 30);
    LocalDateTime tomorrow = today.plusDays(1).atTime(10, 0);
    LocalDateTime yesterday = today.minusDays(1).atTime(10, 0);

    createFollowUp(1L, "Today morning", todayMorning, FollowUpStatus.PENDING);
    createFollowUp(2L, "Today afternoon", todayAfternoon, FollowUpStatus.PENDING);
    createFollowUp(3L, "Tomorrow", tomorrow, FollowUpStatus.PENDING);
    createFollowUp(4L, "Yesterday", yesterday, FollowUpStatus.PENDING);
    createFollowUp(5L, "Today completed", todayMorning, FollowUpStatus.COMPLETED);

    // When & Then
    mockMvc.perform(get("/api/v1/follow-ups/today")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].title", is("Today morning")))
        .andExpect(jsonPath("$[1].title", is("Today afternoon")))
        .andExpect(jsonPath("$[0].status", is("PENDING")))
        .andExpect(jsonPath("$[1].status", is("PENDING")));
  }

  /**
   * Test get today's follow-ups when none exist.
   * Requirement 5.2: Should return empty list when no follow-ups for today
   */
  @Test
  @DisplayName("Should return empty list when no follow-ups for today")
  @WithMockUser(authorities = {"USER"})
  void testGetTodaysFollowUpsEmpty() throws Exception {
    // Given - Create follow-ups for other days
    LocalDateTime tomorrow = LocalDate.now().plusDays(1).atTime(10, 0);
    createFollowUp(1L, "Tomorrow", tomorrow, FollowUpStatus.PENDING);

    // When & Then
    mockMvc.perform(get("/api/v1/follow-ups/today")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  /**
   * Test get overdue follow-ups.
   * Requirement 5.3: WHEN a user requests overdue follow-ups,
   * THE CRM_System SHALL return all incomplete follow-ups with due dates before the current date
   */
  @Test
  @DisplayName("Should get overdue follow-ups")
  @WithMockUser(authorities = {"USER"})
  void testGetOverdueFollowUps() throws Exception {
    // Given - Create follow-ups with different due dates
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime twoDaysAgo = now.minusDays(2);
    LocalDateTime yesterday = now.minusDays(1);
    LocalDateTime tomorrow = now.plusDays(1);

    createFollowUp(1L, "Two days overdue", twoDaysAgo, FollowUpStatus.PENDING);
    createFollowUp(2L, "One day overdue", yesterday, FollowUpStatus.PENDING);
    createFollowUp(3L, "Future follow-up", tomorrow, FollowUpStatus.PENDING);
    createFollowUp(4L, "Overdue but completed", twoDaysAgo, FollowUpStatus.COMPLETED);

    // When & Then
    mockMvc.perform(get("/api/v1/follow-ups/overdue")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].title", is("Two days overdue")))
        .andExpect(jsonPath("$[1].title", is("One day overdue")))
        .andExpect(jsonPath("$[0].status", is("PENDING")))
        .andExpect(jsonPath("$[1].status", is("PENDING")))
        .andExpect(jsonPath("$[0].overdue", is(true)))
        .andExpect(jsonPath("$[1].overdue", is(true)));
  }

  /**
   * Test get overdue follow-ups when none exist.
   * Requirement 5.3: Should return empty list when no overdue follow-ups
   */
  @Test
  @DisplayName("Should return empty list when no overdue follow-ups")
  @WithMockUser(authorities = {"USER"})
  void testGetOverdueFollowUpsEmpty() throws Exception {
    // Given - Create only future follow-ups
    LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
    createFollowUp(1L, "Future follow-up", tomorrow, FollowUpStatus.PENDING);

    // When & Then
    mockMvc.perform(get("/api/v1/follow-ups/overdue")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  /**
   * Test mark follow-up as completed.
   * Requirement 5.4: WHEN a user marks a follow-up as complete,
   * THE CRM_System SHALL update the completion status and record the completion timestamp
   */
  @Test
  @DisplayName("Should mark follow-up as completed")
  @WithMockUser(authorities = {"USER"})
  void testCompleteFollowUp() throws Exception {
    // Given - Create a pending follow-up
    LocalDateTime dueDate = LocalDateTime.now().plusDays(1);
    FollowUp followUp = createFollowUp(1L, "Follow-up to complete", dueDate, FollowUpStatus.PENDING);

    // When & Then
    mockMvc.perform(put("/api/v1/follow-ups/{id}/complete", followUp.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(followUp.getId().intValue())))
        .andExpect(jsonPath("$.status", is("COMPLETED")))
        .andExpect(jsonPath("$.completedAt", notNullValue()))
        .andExpect(jsonPath("$.overdue", is(false)));

    // Verify the follow-up was updated in the database
    FollowUp updated = followUpRepository.findById(followUp.getId()).orElseThrow();
    assert updated.getStatus() == FollowUpStatus.COMPLETED;
    assert updated.getCompletedAt() != null;
  }

  /**
   * Test mark non-existent follow-up as completed.
   * Requirement 5.4: Should return 404 when follow-up doesn't exist
   */
  @Test
  @DisplayName("Should return 404 when completing non-existent follow-up")
  @WithMockUser(authorities = {"USER"})
  void testCompleteNonExistentFollowUp() throws Exception {
    // When & Then
    mockMvc.perform(put("/api/v1/follow-ups/{id}/complete", 99999L)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  /**
   * Test list follow-ups with pagination.
   * Requirement 5.2: Follow-ups should support pagination
   */
  @Test
  @DisplayName("Should list follow-ups with pagination")
  @WithMockUser(authorities = {"USER"})
  void testListFollowUps() throws Exception {
    // Given - Create multiple follow-ups
    LocalDateTime baseDate = LocalDateTime.now().plusDays(1);
    for (int i = 1; i <= 5; i++) {
      createFollowUp((long) i, "Follow-up " + i, baseDate.plusHours(i), FollowUpStatus.PENDING);
    }

    // When & Then
    mockMvc.perform(get("/api/v1/follow-ups")
            .param("page", "0")
            .param("size", "3")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.totalElements", is(5)))
        .andExpect(jsonPath("$.totalPages", is(2)))
        .andExpect(jsonPath("$.number", is(0)))
        .andExpect(jsonPath("$.size", is(3)));
  }

  /**
   * Test delete follow-up.
   * Requirement 5.6: WHEN a user deletes a follow-up,
   * THE CRM_System SHALL remove it from the database
   */
  @Test
  @DisplayName("Should delete follow-up")
  @WithMockUser(authorities = {"USER"})
  void testDeleteFollowUp() throws Exception {
    // Given
    LocalDateTime dueDate = LocalDateTime.now().plusDays(1);
    FollowUp followUp = createFollowUp(1L, "Follow-up to delete", dueDate, FollowUpStatus.PENDING);

    // When & Then
    mockMvc.perform(delete("/api/v1/follow-ups/{id}", followUp.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    // Verify the follow-up was deleted from the database
    assert followUpRepository.findById(followUp.getId()).isEmpty();
  }

  /**
   * Test delete non-existent follow-up.
   * Requirement 5.6: Should return 404 when follow-up doesn't exist
   */
  @Test
  @DisplayName("Should return 404 when deleting non-existent follow-up")
  @WithMockUser(authorities = {"USER"})
  void testDeleteNonExistentFollowUp() throws Exception {
    // When & Then
    mockMvc.perform(delete("/api/v1/follow-ups/{id}", 99999L)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  /**
   * Test update follow-up.
   * Requirement 5.5: WHEN a user updates a follow-up's due date or description,
   * THE CRM_System SHALL save the changes
   */
  @Test
  @DisplayName("Should update follow-up")
  @WithMockUser(authorities = {"USER"})
  void testUpdateFollowUp() throws Exception {
    // Given
    LocalDateTime originalDueDate = LocalDateTime.now().plusDays(1);
    FollowUp followUp = createFollowUp(1L, "Original title", originalDueDate, FollowUpStatus.PENDING);

    LocalDateTime newDueDate = LocalDateTime.now().plusDays(3);
    FollowUpDtos.UpdateFollowUpRequest request = new FollowUpDtos.UpdateFollowUpRequest(
        "Updated title",
        "Updated description",
        newDueDate,
        FollowUpPriority.LOW,
        "newUser"
    );

    // When & Then
    mockMvc.perform(put("/api/v1/follow-ups/{id}", followUp.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(followUp.getId().intValue())))
        .andExpect(jsonPath("$.title", is("Updated title")))
        .andExpect(jsonPath("$.description", is("Updated description")))
        .andExpect(jsonPath("$.priority", is("LOW")))
        .andExpect(jsonPath("$.assignedTo", is("newUser")));

    // Verify the follow-up was updated in the database
    FollowUp updated = followUpRepository.findById(followUp.getId()).orElseThrow();
    assert updated.getTitle().equals("Updated title");
    assert updated.getDescription().equals("Updated description");
    assert updated.getPriority() == FollowUpPriority.LOW;
  }

  // Helper methods

  private FollowUp createFollowUp(Long leadId, String title, LocalDateTime dueDate, FollowUpStatus status) {
    FollowUp followUp = new FollowUp();
    followUp.setLeadId(leadId);
    followUp.setTitle(title);
    followUp.setDescription("Test description for " + title);
    followUp.setDueDate(dueDate);
    followUp.setPriority(FollowUpPriority.MEDIUM);
    followUp.setStatus(status);
    followUp.setCreatedBy("test-user");
    followUp.setUpdatedBy("test-user");

    if (status == FollowUpStatus.COMPLETED) {
      followUp.setCompletedAt(LocalDateTime.now());
    }

    return followUpRepository.save(followUp);
  }
}
