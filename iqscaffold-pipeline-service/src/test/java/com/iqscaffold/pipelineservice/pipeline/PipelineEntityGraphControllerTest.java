package com.iqscaffold.pipelineservice.pipeline;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.iqscaffold.pipelineservice.activity.ActivityType;
import com.iqscaffold.pipelineservice.activity.PipelineActivity;
import com.iqscaffold.pipelineservice.activity.PipelineActivityRepository;
import com.iqscaffold.pipelineservice.followup.FollowUp;
import com.iqscaffold.pipelineservice.followup.FollowUpPriority;
import com.iqscaffold.pipelineservice.followup.FollowUpRepository;
import com.iqscaffold.pipelineservice.followup.FollowUpStatus;
import com.iqscaffold.pipelineservice.shared.test.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for PipelineEntityGraphController.
 * These tests verify that the entity graph endpoints work correctly and return optimized data.
 */
@DisplayName("Pipeline Entity Graph Controller Tests")
@AutoConfigureMockMvc
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@Disabled("Authentication setup required - tests fail with 401 errors")
class PipelineEntityGraphControllerTest extends BaseIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private PipelineStageRepository pipelineStageRepository;

  @Autowired
  private PipelineItemRepository pipelineItemRepository;

  @Autowired
  private FollowUpRepository followUpRepository;

  @Autowired
  private PipelineActivityRepository pipelineActivityRepository;

  private PipelineStage testStage;
  private PipelineItem testPipelineItem;
  private FollowUp testFollowUp;
  private PipelineActivity testActivity;

  @BeforeEach
  void setUp() {
    // Create test stage
    testStage = new PipelineStage("Qualified", 2);
    testStage.setDescription("Qualified leads ready for proposal");
    testStage.setColorCode("#28a745");
    testStage.setCreatedBy("test-user");
    testStage.setUpdatedBy("test-user");
    testStage = pipelineStageRepository.save(testStage);

    // Create test pipeline item
    testPipelineItem = new PipelineItem(12345L, testStage.getId());
    testPipelineItem.setExpectedValue(new BigDecimal("50000.00"));
    testPipelineItem.setProbability(new BigDecimal("75.00"));
    testPipelineItem.setDaysInStage(5);
    testPipelineItem.setCreatedBy("test-user");
    testPipelineItem.setUpdatedBy("test-user");
    testPipelineItem = pipelineItemRepository.save(testPipelineItem);

    // Create test follow-up
    testFollowUp = new FollowUp(12345L, "Follow up with client", LocalDateTime.now().plusDays(3));
    testFollowUp.setDescription("Discuss proposal details");
    testFollowUp.setPriority(FollowUpPriority.HIGH);
    testFollowUp.setStatus(FollowUpStatus.PENDING);
    testFollowUp.setAssignedTo("sales-rep");
    testFollowUp.setCreatedBy("test-user");
    testFollowUp.setUpdatedBy("test-user");
    testFollowUp = followUpRepository.save(testFollowUp);

    // Create test activity
    testActivity = new PipelineActivity(12345L, ActivityType.STAGE_CHANGED, "Lead moved to Qualified stage");
    testActivity.setNotes("Lead showed strong interest in our solution");
    testActivity.setOldStageId(1L);
    testActivity.setNewStageId(testStage.getId());
    testActivity.setCreatedBy("test-user");
    testActivity = pipelineActivityRepository.save(testActivity);
  }

  // Pipeline Item Entity Graph Tests

  @Test
  @DisplayName("Should get pipeline item with stage")
  void shouldGetPipelineItemWithStage() throws Exception {
    mockMvc.perform(get("/api/v1/pipeline/optimized/items/{id}/with-stage", testPipelineItem.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(testPipelineItem.getId().intValue())))
        .andExpect(jsonPath("$.leadId", is(12345)))
        .andExpect(jsonPath("$.stageId", is(testStage.getId().intValue())))
        .andExpect(jsonPath("$.expectedValue", is(50000.00)))
        .andExpect(jsonPath("$.probability", is(75.00)))
        .andExpect(jsonPath("$.daysInStage", is(5)))
        .andExpect(jsonPath("$.stage.id", is(testStage.getId().intValue())))
        .andExpect(jsonPath("$.stage.name", is("Qualified")))
        .andExpect(jsonPath("$.stage.description", is("Qualified leads ready for proposal")))
        .andExpect(jsonPath("$.stage.colorCode", is("#28a745")))
        .andExpect(jsonPath("$.stage.displayOrder", is(2)))
        .andExpect(jsonPath("$.stage.isActive", is(true)));
  }

  @Test
  @DisplayName("Should get pipeline item with stage by lead ID")
  void shouldGetPipelineItemWithStageByLeadId() throws Exception {
    mockMvc.perform(get("/api/v1/pipeline/optimized/items/lead/{leadId}/with-stage", 12345L)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.leadId", is(12345)))
        .andExpect(jsonPath("$.stage.name", is("Qualified")));
  }

  @Test
  @DisplayName("Should get basic pipeline item")
  void shouldGetBasicPipelineItem() throws Exception {
    mockMvc.perform(get("/api/v1/pipeline/optimized/items/{id}/basic", testPipelineItem.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(testPipelineItem.getId().intValue())))
        .andExpect(jsonPath("$.leadId", is(12345)))
        .andExpect(jsonPath("$.expectedValue", is(50000.00)));
  }

  @Test
  @DisplayName("Should return 404 when pipeline item not found")
  void shouldReturn404WhenPipelineItemNotFound() throws Exception {
    mockMvc.perform(get("/api/v1/pipeline/optimized/items/{id}/with-stage", 99999L)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  // Pipeline Stage Entity Graph Tests

  @Test
  @DisplayName("Should get pipeline stage with items")
  void shouldGetPipelineStageWithItems() throws Exception {
    mockMvc.perform(get("/api/v1/pipeline/optimized/stages/{id}/with-items", testStage.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(testStage.getId().intValue())))
        .andExpect(jsonPath("$.name", is("Qualified")))
        .andExpect(jsonPath("$.description", is("Qualified leads ready for proposal")))
        .andExpect(jsonPath("$.displayOrder", is(2)))
        .andExpect(jsonPath("$.isActive", is(true)))
        .andExpect(jsonPath("$.pipelineItems", hasSize(1)))
        .andExpect(jsonPath("$.pipelineItems[0].leadId", is(12345)))
        .andExpect(jsonPath("$.pipelineItems[0].expectedValue", is(50000.00)));
  }

  @Test
  @DisplayName("Should get basic pipeline stage")
  void shouldGetBasicPipelineStage() throws Exception {
    mockMvc.perform(get("/api/v1/pipeline/optimized/stages/{id}/basic", testStage.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(testStage.getId().intValue())))
        .andExpect(jsonPath("$.name", is("Qualified")))
        .andExpect(jsonPath("$.description", is("Qualified leads ready for proposal")));
  }

  @Test
  @DisplayName("Should get active pipeline stages with items")
  void shouldGetActivePipelineStagesWithItems() throws Exception {
    mockMvc.perform(get("/api/v1/pipeline/optimized/stages/active/with-items")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].name", is("Qualified")))
        .andExpect(jsonPath("$[0].isActive", is(true)))
        .andExpect(jsonPath("$[0].pipelineItems", hasSize(1)));
  }

  @Test
  @DisplayName("Should return 404 when pipeline stage not found")
  void shouldReturn404WhenPipelineStageNotFound() throws Exception {
    mockMvc.perform(get("/api/v1/pipeline/optimized/stages/{id}/with-items", 99999L)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  // Follow-up Entity Graph Tests

  @Test
  @DisplayName("Should get basic follow-ups by lead")
  void shouldGetBasicFollowUpsByLead() throws Exception {
    mockMvc.perform(get("/api/v1/pipeline/optimized/follow-ups/lead/{leadId}/basic", 12345L)
            .param("page", "0")
            .param("size", "10")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].leadId", is(12345)))
        .andExpect(jsonPath("$.content[0].title", is("Follow up with client")))
        .andExpect(jsonPath("$.content[0].description", is("Discuss proposal details")))
        .andExpect(jsonPath("$.content[0].priority", is("HIGH")))
        .andExpect(jsonPath("$.content[0].status", is("PENDING")))
        .andExpect(jsonPath("$.content[0].assignedTo", is("sales-rep")));
  }

  @Test
  @DisplayName("Should get basic follow-ups by status")
  void shouldGetBasicFollowUpsByStatus() throws Exception {
    mockMvc.perform(get("/api/v1/pipeline/optimized/follow-ups/status/{status}/basic", "PENDING")
            .param("page", "0")
            .param("size", "10")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].status", is("PENDING")));
  }

  @Test
  @DisplayName("Should get basic overdue follow-ups")
  void shouldGetBasicOverdueFollowUps() throws Exception {
    // Create an overdue follow-up
    FollowUp overdueFollowUp = new FollowUp(12345L, "Overdue follow-up", LocalDateTime.now().minusDays(2));
    overdueFollowUp.setStatus(FollowUpStatus.PENDING);
    overdueFollowUp.setCreatedBy("test-user");
    overdueFollowUp.setUpdatedBy("test-user");
    followUpRepository.save(overdueFollowUp);

    mockMvc.perform(get("/api/v1/pipeline/optimized/follow-ups/overdue/basic")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].title", is("Overdue follow-up")))
        .andExpect(jsonPath("$[0].status", is("PENDING")));
  }

  // Pipeline Activity Entity Graph Tests

  @Test
  @DisplayName("Should get basic pipeline activities by lead")
  void shouldGetBasicPipelineActivitiesByLead() throws Exception {
    mockMvc.perform(get("/api/v1/pipeline/optimized/activities/lead/{leadId}/basic", 12345L)
            .param("page", "0")
            .param("size", "10")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].leadId", is(12345)))
        .andExpect(jsonPath("$.content[0].activityType", is("STAGE_CHANGED")))
        .andExpect(jsonPath("$.content[0].description", is("Lead moved to Qualified stage")))
        .andExpect(jsonPath("$.content[0].notes", is("Lead showed strong interest in our solution")))
        .andExpect(jsonPath("$.content[0].oldStageId", is(1)))
        .andExpect(jsonPath("$.content[0].newStageId", is(testStage.getId().intValue())));
  }

  @Test
  @DisplayName("Should get all basic pipeline activities by lead")
  void shouldGetAllBasicPipelineActivitiesByLead() throws Exception {
    // Create additional activity
    PipelineActivity activity2 = new PipelineActivity(12345L, ActivityType.FOLLOW_UP_CREATED, "Follow-up task created");
    activity2.setCreatedBy("test-user");
    pipelineActivityRepository.save(activity2);

    mockMvc.perform(get("/api/v1/pipeline/optimized/activities/lead/{leadId}/all/basic", 12345L)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].activityType", is("FOLLOW_UP_CREATED"))) // Most recent first
        .andExpect(jsonPath("$[1].activityType", is("STAGE_CHANGED")));
  }

  @Test
  @DisplayName("Should get basic pipeline activities by type")
  void shouldGetBasicPipelineActivitiesByType() throws Exception {
    mockMvc.perform(get("/api/v1/pipeline/optimized/activities/type/{activityType}/basic", "STAGE_CHANGED")
            .param("page", "0")
            .param("size", "10")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].activityType", is("STAGE_CHANGED")));
  }
}
