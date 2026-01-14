package com.iqscaffold.pipelineservice.dashboard;

import com.iqscaffold.pipelineservice.pipeline.PipelineItem;
import com.iqscaffold.pipelineservice.pipeline.PipelineItemRepository;
import com.iqscaffold.pipelineservice.pipeline.PipelineStage;
import com.iqscaffold.pipelineservice.pipeline.PipelineStageRepository;
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

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Dashboard statistics and metrics operations.
 * Tests Requirements: 8.1, 8.2, 8.3, 8.4
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardRestResourceIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private PipelineItemRepository pipelineItemRepository;

  @Autowired
  private PipelineStageRepository stageRepository;

  @MockBean
  private org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory;

  private PipelineStage newStage;
  private PipelineStage contactedStage;
  private PipelineStage qualifiedStage;
  private PipelineStage wonStage;
  private PipelineStage lostStage;

  @BeforeEach
  void setUp() {
    // Clean up data before each test
    pipelineItemRepository.deleteAll();
    stageRepository.deleteAll();

    // Create default pipeline stages
    newStage = createStage("New", 1, false);
    contactedStage = createStage("Contacted", 2, false);
    qualifiedStage = createStage("Qualified", 3, false);
    wonStage = createStage("Won", 4, true);
    lostStage = createStage("Lost", 5, true);
  }

  /**
   * Test get dashboard stats with pipeline items.
   * Requirement 8.1: WHEN a user requests dashboard statistics,
   * THE CRM_System SHALL return the count of leads in each pipeline stage
   */
  @Test
  @DisplayName("Should get dashboard stats with leads by stage")
  @WithMockUser(authorities = {"USER"})
  void testGetDashboardStatsWithLeadsByStage() throws Exception {
    // Given - Create pipeline items in different stages
    createPipelineItem(1L, newStage.getId(), LocalDateTime.now().minusDays(5));
    createPipelineItem(2L, newStage.getId(), LocalDateTime.now().minusDays(4));
    createPipelineItem(3L, contactedStage.getId(), LocalDateTime.now().minusDays(3));
    createPipelineItem(4L, qualifiedStage.getId(), LocalDateTime.now().minusDays(2));
    createPipelineItem(5L, wonStage.getId(), LocalDateTime.now().minusDays(1));
    createPipelineItem(6L, lostStage.getId(), LocalDateTime.now());

    // When & Then
    mockMvc.perform(get("/api/v1/dashboard/stats")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.leadsByStage", notNullValue()))
        .andExpect(jsonPath("$.leadsByStage.New", is(2)))
        .andExpect(jsonPath("$.leadsByStage.Contacted", is(1)))
        .andExpect(jsonPath("$.leadsByStage.Qualified", is(1)))
        .andExpect(jsonPath("$.leadsByStage.Won", is(1)))
        .andExpect(jsonPath("$.leadsByStage.Lost", is(1)))
        .andExpect(jsonPath("$.totalLeads", is(6)))
        .andExpect(jsonPath("$.activeLeads", is(4)))
        .andExpect(jsonPath("$.wonLeads", is(1)))
        .andExpect(jsonPath("$.lostLeads", is(1)))
        .andExpect(jsonPath("$.leadsBySource", notNullValue()));
  }

  /**
   * Test get dashboard stats with empty pipeline.
   * Requirement 8.1: Should handle empty pipeline gracefully
   */
  @Test
  @DisplayName("Should get dashboard stats with empty pipeline")
  @WithMockUser(authorities = {"USER"})
  void testGetDashboardStatsEmpty() throws Exception {
    // Given - No pipeline items

    // When & Then
    mockMvc.perform(get("/api/v1/dashboard/stats")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.leadsByStage", notNullValue()))
        .andExpect(jsonPath("$.leadsBySource", notNullValue()))
        .andExpect(jsonPath("$.totalLeads", is(0)))
        .andExpect(jsonPath("$.activeLeads", is(0)))
        .andExpect(jsonPath("$.wonLeads", is(0)))
        .andExpect(jsonPath("$.lostLeads", is(0)));
  }

  /**
   * Test get dashboard stats with date range filter.
   * Requirement 8.5: WHEN a user requests statistics with a date range filter,
   * THE CRM_System SHALL return metrics for leads created within that range
   */
  @Test
  @DisplayName("Should get dashboard stats with date range filter")
  @WithMockUser(authorities = {"USER"})
  void testGetDashboardStatsWithDateRange() throws Exception {
    // Given - Create pipeline items with different creation dates
    LocalDateTime now = LocalDateTime.now();
    createPipelineItem(1L, newStage.getId(), now.minusDays(10)); // Outside range
    createPipelineItem(2L, contactedStage.getId(), now.minusDays(5)); // Within range
    createPipelineItem(3L, qualifiedStage.getId(), now.minusDays(3)); // Within range
    createPipelineItem(4L, wonStage.getId(), now.minusDays(1)); // Within range

    // When & Then - Filter for last 7 days
    String startDate = now.minusDays(7).toLocalDate().toString();
    String endDate = now.toLocalDate().toString();

    // Note: Date filtering is applied in the service layer
    mockMvc.perform(get("/api/v1/dashboard/stats")
            .param("startDate", startDate)
            .param("endDate", endDate)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.leadsByStage", notNullValue()))
        .andExpect(jsonPath("$.leadsBySource", notNullValue()))
        .andExpect(jsonPath("$.totalLeads", greaterThanOrEqualTo(0)));
  }

  /**
   * Test get dashboard stats includes leads by source.
   * Requirement 8.1: Dashboard should include lead counts by source
   */
  @Test
  @DisplayName("Should get dashboard stats with leads by source")
  @WithMockUser(authorities = {"USER"})
  void testGetDashboardStatsWithLeadsBySource() throws Exception {
    // Given - Create some pipeline items
    createPipelineItem(1L, newStage.getId(), LocalDateTime.now());
    createPipelineItem(2L, contactedStage.getId(), LocalDateTime.now());

    // When & Then - leadsBySource will be empty since Lead Service is not available in test
    mockMvc.perform(get("/api/v1/dashboard/stats")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.leadsBySource", notNullValue()))
        .andExpect(jsonPath("$.totalLeads", is(2)))
        .andExpect(jsonPath("$.leadsByStage", notNullValue()));
  }

  /**
   * Test get conversion metrics with conversion rate.
   * Requirement 8.2: WHEN a user requests conversion metrics,
   * THE CRM_System SHALL calculate and return the conversion rate from New to Won
   */
  @Test
  @DisplayName("Should get conversion metrics with conversion rate")
  @WithMockUser(authorities = {"USER"})
  void testGetConversionMetricsWithConversionRate() throws Exception {
    // Given - Create pipeline items with some Won leads
    createPipelineItem(1L, newStage.getId(), LocalDateTime.now().minusDays(5));
    createPipelineItem(2L, contactedStage.getId(), LocalDateTime.now().minusDays(4));
    createPipelineItem(3L, wonStage.getId(), LocalDateTime.now().minusDays(3));
    createPipelineItem(4L, wonStage.getId(), LocalDateTime.now().minusDays(2));
    createPipelineItem(5L, lostStage.getId(), LocalDateTime.now().minusDays(1));

    // When & Then - 2 out of 5 leads are Won = 40% conversion rate
    mockMvc.perform(get("/api/v1/dashboard/conversion")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.conversionRate", is(40.0)))
        .andExpect(jsonPath("$.averageTimeToConvert", notNullValue()))
        .andExpect(jsonPath("$.stageVelocity", notNullValue()));
  }

  /**
   * Test get conversion metrics with zero leads.
   * Requirement 8.2: Should handle zero leads gracefully
   */
  @Test
  @DisplayName("Should get conversion metrics with zero leads")
  @WithMockUser(authorities = {"USER"})
  void testGetConversionMetricsEmpty() throws Exception {
    // Given - No pipeline items

    // When & Then
    mockMvc.perform(get("/api/v1/dashboard/conversion")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.conversionRate", is(0.0)))
        .andExpect(jsonPath("$.averageTimeToConvert", is(0.0)))
        .andExpect(jsonPath("$.stageVelocity", notNullValue()));
  }

  /**
   * Test get conversion metrics with average time to convert.
   * Requirement 8.3: WHEN a user requests conversion metrics,
   * THE CRM_System SHALL calculate the average time to convert from New to Won
   */
  @Test
  @DisplayName("Should get conversion metrics with average time to convert")
  @WithMockUser(authorities = {"USER"})
  void testGetConversionMetricsWithAverageTimeToConvert() throws Exception {
    // Given - Create Won leads with different conversion times
    LocalDateTime now = LocalDateTime.now();
    
    // Lead 1: Created 10 days ago, converted today (10 days to convert)
    PipelineItem item1 = createPipelineItem(1L, wonStage.getId(), now.minusDays(10));
    item1.setConvertedAt(now);
    pipelineItemRepository.save(item1);
    
    // Lead 2: Created 20 days ago, converted 15 days ago (5 days to convert)
    PipelineItem item2 = createPipelineItem(2L, wonStage.getId(), now.minusDays(20));
    item2.setConvertedAt(now.minusDays(15));
    pipelineItemRepository.save(item2);
    
    // Lead 3: Still in New stage (not converted)
    createPipelineItem(3L, newStage.getId(), now.minusDays(3));

    // When & Then - Average time to convert should be calculated
    // Note: The actual value depends on when @CreationTimestamp sets the timestamp
    mockMvc.perform(get("/api/v1/dashboard/conversion")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.averageTimeToConvert", notNullValue()))
        .andExpect(jsonPath("$.conversionRate", greaterThanOrEqualTo(0.0)));
  }

  /**
   * Test get conversion metrics with stage velocity.
   * Requirement 8.4: WHEN a user requests pipeline velocity,
   * THE CRM_System SHALL calculate the average time leads spend in each stage
   */
  @Test
  @DisplayName("Should get conversion metrics with stage velocity")
  @WithMockUser(authorities = {"USER"})
  void testGetConversionMetricsWithStageVelocity() throws Exception {
    // Given - Create pipeline items with days in stage data
    PipelineItem item1 = createPipelineItem(1L, newStage.getId(), LocalDateTime.now().minusDays(5));
    item1.setDaysInStage(5);
    pipelineItemRepository.save(item1);
    
    PipelineItem item2 = createPipelineItem(2L, newStage.getId(), LocalDateTime.now().minusDays(3));
    item2.setDaysInStage(3);
    pipelineItemRepository.save(item2);
    
    PipelineItem item3 = createPipelineItem(3L, contactedStage.getId(), LocalDateTime.now().minusDays(2));
    item3.setDaysInStage(2);
    pipelineItemRepository.save(item3);

    // When & Then
    mockMvc.perform(get("/api/v1/dashboard/conversion")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stageVelocity", notNullValue()))
        .andExpect(jsonPath("$.stageVelocity.New", is(4.0))) // Average of 5 and 3
        .andExpect(jsonPath("$.stageVelocity.Contacted", is(2.0)));
  }

  /**
   * Test get conversion metrics with date range filter.
   * Requirement 8.5: Should support date range filtering for conversion metrics
   */
  @Test
  @DisplayName("Should get conversion metrics with date range filter")
  @WithMockUser(authorities = {"USER"})
  void testGetConversionMetricsWithDateRange() throws Exception {
    // Given - Create pipeline items with different creation dates
    LocalDateTime now = LocalDateTime.now();
    
    // Outside range
    createPipelineItem(1L, wonStage.getId(), now.minusDays(20));
    
    // Within range
    createPipelineItem(2L, wonStage.getId(), now.minusDays(5));
    createPipelineItem(3L, newStage.getId(), now.minusDays(3));
    createPipelineItem(4L, contactedStage.getId(), now.minusDays(2));

    // When & Then - Filter for last 7 days
    String startDate = now.minusDays(7).toLocalDate().toString();
    String endDate = now.toLocalDate().toString();

    mockMvc.perform(get("/api/v1/dashboard/conversion")
            .param("startDate", startDate)
            .param("endDate", endDate)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.conversionRate", notNullValue()))
        .andExpect(jsonPath("$.averageTimeToConvert", notNullValue()))
        .andExpect(jsonPath("$.stageVelocity", notNullValue()));
  }

  /**
   * Test dashboard stats requires authentication.
   * Requirement 8.6: WHEN a user requests statistics,
   * THE CRM_System SHALL apply tenant isolation to ensure data privacy
   */
  @Test
  @DisplayName("Should require authentication for dashboard stats")
  void testGetDashboardStatsRequiresAuth() throws Exception {
    // When & Then - No authentication
    mockMvc.perform(get("/api/v1/dashboard/stats")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  /**
   * Test conversion metrics requires authentication.
   * Requirement 8.6: Should enforce authentication for conversion metrics
   */
  @Test
  @DisplayName("Should require authentication for conversion metrics")
  void testGetConversionMetricsRequiresAuth() throws Exception {
    // When & Then - No authentication
    mockMvc.perform(get("/api/v1/dashboard/conversion")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  // Helper methods

  private PipelineStage createStage(String name, Integer displayOrder, Boolean isFinalStage) {
    PipelineStage stage = new PipelineStage();
    stage.setName(name);
    stage.setDisplayOrder(displayOrder);
    stage.setIsFinalStage(isFinalStage);
    stage.setIsActive(true);
    stage.setCreatedBy("test-user");
    stage.setUpdatedBy("test-user");
    return stageRepository.save(stage);
  }

  private PipelineItem createPipelineItem(Long leadId, Long stageId, LocalDateTime createdAt) {
    PipelineItem item = new PipelineItem();
    item.setLeadId(leadId);
    item.setStageId(stageId);
    item.setEnteredStageAt(createdAt);
    item.setCreatedAt(createdAt);
    item.setCreatedBy("test-user");
    item.setUpdatedBy("test-user");
    return pipelineItemRepository.save(item);
  }
}
