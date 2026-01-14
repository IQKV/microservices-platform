package com.iqscaffold.pipelineservice.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqscaffold.pipelineservice.pipeline.dto.PipelineItemDtos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Pipeline Item management operations.
 * Tests Requirements: 4.1, 4.2, 4.3, 4.6
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PipelineItemRestResourceIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private PipelineItemRepository itemRepository;

  @Autowired
  private PipelineStageRepository stageRepository;

  @MockBean
  private RabbitTemplate rabbitTemplate;

  @MockBean
  private org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory;

  private PipelineStage newStage;
  private PipelineStage contactedStage;
  private PipelineStage wonStage;

  @BeforeEach
  void setUp() {
    // Clean up data before each test
    itemRepository.deleteAll();
    stageRepository.deleteAll();

    // Create test stages
    newStage = createStage("New", 0, false);
    contactedStage = createStage("Contacted", 1, false);
    wonStage = createStage("Won", 4, true);
  }

  /**
   * Test list pipeline items.
   * Requirement 4.1: WHEN a lead is created, THE CRM_System SHALL automatically assign it to the "New" pipeline stage
   * Requirement 4.2: WHEN a user moves a lead to a different stage, THE CRM_System SHALL update the lead's current stage
   */
  @Test
  @DisplayName("Should list all pipeline items")
  @WithMockUser(authorities = {"USER"})
  void testListPipelineItems() throws Exception {
    // Given - Create multiple pipeline items
    createPipelineItem(1L, newStage.getId());
    createPipelineItem(2L, newStage.getId());
    createPipelineItem(3L, contactedStage.getId());

    // When & Then
    mockMvc.perform(get("/api/v1/pipeline/items")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.totalElements", is(3)))
        .andExpect(jsonPath("$.content[0].leadId", notNullValue()))
        .andExpect(jsonPath("$.content[0].stageId", notNullValue()))
        .andExpect(jsonPath("$.content[0].enteredStageAt", notNullValue()));
  }

  /**
   * Test list pipeline items filtered by stage.
   * Requirement 4.1: Pipeline items should be filterable by stage
   */
  @Test
  @DisplayName("Should list pipeline items filtered by stage")
  @WithMockUser(authorities = {"USER"})
  void testListPipelineItemsByStage() throws Exception {
    // Given - Create pipeline items in different stages
    createPipelineItem(1L, newStage.getId());
    createPipelineItem(2L, newStage.getId());
    createPipelineItem(3L, contactedStage.getId());

    // When & Then - Filter by "New" stage
    mockMvc.perform(get("/api/v1/pipeline/items")
            .param("stageId", newStage.getId().toString())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.totalElements", is(2)))
        .andExpect(jsonPath("$.content[0].stageId", is(newStage.getId().intValue())))
        .andExpect(jsonPath("$.content[1].stageId", is(newStage.getId().intValue())));
  }

  /**
   * Test get pipeline item by ID.
   * Requirement 4.1: Should retrieve a specific pipeline item
   */
  @Test
  @DisplayName("Should get pipeline item by ID")
  @WithMockUser(authorities = {"USER"})
  void testGetPipelineItemById() throws Exception {
    // Given
    PipelineItem item = createPipelineItem(1L, newStage.getId());

    // When & Then
    mockMvc.perform(get("/api/v1/pipeline/items/{id}", item.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(item.getId().intValue())))
        .andExpect(jsonPath("$.leadId", is(1)))
        .andExpect(jsonPath("$.stageId", is(newStage.getId().intValue())))
        .andExpect(jsonPath("$.enteredStageAt", notNullValue()));
  }

  /**
   * Test get pipeline item by ID - not found.
   * Requirement 4.1: Should return 404 when pipeline item doesn't exist
   */
  @Test
  @DisplayName("Should return 404 when pipeline item not found")
  @WithMockUser(authorities = {"USER"})
  void testGetPipelineItemNotFound() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/v1/pipeline/items/{id}", 99999L)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  /**
   * Test move lead to different stage.
   * Requirement 4.2: WHEN a user moves a lead to a different stage,
   * THE CRM_System SHALL update the lead's current stage and log the transition in the Activity_Log
   * Requirement 4.6: WHEN a lead transitions between stages,
   * THE CRM_System SHALL update the last_modified timestamp
   */
  @Test
  @DisplayName("Should move lead to different stage")
  @WithMockUser(authorities = {"USER"})
  void testMoveLeadToStage() throws Exception {
    // Given - Create a pipeline item in "New" stage
    PipelineItem item = createPipelineItem(1L, newStage.getId());
    LocalDateTime originalEnteredStageAt = item.getEnteredStageAt();

    // Wait a bit to ensure timestamp changes
    Thread.sleep(10);

    // Move to "Contacted" stage
    PipelineItemDtos.MoveToStageRequest request = new PipelineItemDtos.MoveToStageRequest(
        contactedStage.getId()
    );

    // When & Then
    mockMvc.perform(put("/api/v1/pipeline/items/{id}/stage", item.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(item.getId().intValue())))
        .andExpect(jsonPath("$.leadId", is(1)))
        .andExpect(jsonPath("$.stageId", is(contactedStage.getId().intValue())))
        .andExpect(jsonPath("$.enteredStageAt", notNullValue()));

    // Verify the item was updated in the database
    PipelineItem updated = itemRepository.findById(item.getId()).orElseThrow();
    assert updated.getStageId().equals(contactedStage.getId());
    assert updated.getEnteredStageAt().isAfter(originalEnteredStageAt);
  }

  /**
   * Test move lead to Won stage sets converted_at timestamp.
   * Requirement 4.3: WHEN a user moves a lead to "Won" stage,
   * THE CRM_System SHALL mark the lead as successfully converted and record the conversion date
   */
  @Test
  @DisplayName("Should set converted_at when moving to Won stage")
  @WithMockUser(authorities = {"USER"})
  void testMoveToWonStageSetsConvertedAt() throws Exception {
    // Given - Create a pipeline item in "New" stage
    PipelineItem item = createPipelineItem(1L, newStage.getId());

    // Move to "Won" stage
    PipelineItemDtos.MoveToStageRequest request = new PipelineItemDtos.MoveToStageRequest(
        wonStage.getId()
    );

    // When & Then
    mockMvc.perform(put("/api/v1/pipeline/items/{id}/stage", item.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(item.getId().intValue())))
        .andExpect(jsonPath("$.stageId", is(wonStage.getId().intValue())))
        .andExpect(jsonPath("$.convertedAt", notNullValue()));

    // Verify the converted_at timestamp was set in the database
    PipelineItem updated = itemRepository.findById(item.getId()).orElseThrow();
    assert updated.getStageId().equals(wonStage.getId());
    assert updated.getConvertedAt() != null;
  }

  /**
   * Test move lead from Won stage to another stage clears converted_at.
   * Requirement 4.3: Moving away from Won stage should clear the conversion date
   */
  @Test
  @DisplayName("Should clear converted_at when moving away from Won stage")
  @WithMockUser(authorities = {"USER"})
  void testMoveFromWonStageClearsConvertedAt() throws Exception {
    // Given - Create a pipeline item in "Won" stage with converted_at set
    PipelineItem item = createPipelineItem(1L, wonStage.getId());
    item.setConvertedAt(LocalDateTime.now());
    itemRepository.save(item);

    // Move back to "Contacted" stage
    PipelineItemDtos.MoveToStageRequest request = new PipelineItemDtos.MoveToStageRequest(
        contactedStage.getId()
    );

    // When & Then
    mockMvc.perform(put("/api/v1/pipeline/items/{id}/stage", item.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(item.getId().intValue())))
        .andExpect(jsonPath("$.stageId", is(contactedStage.getId().intValue())))
        .andExpect(jsonPath("$.convertedAt", nullValue()));

    // Verify the converted_at timestamp was cleared in the database
    PipelineItem updated = itemRepository.findById(item.getId()).orElseThrow();
    assert updated.getStageId().equals(contactedStage.getId());
    assert updated.getConvertedAt() == null;
  }

  /**
   * Test move lead to invalid stage.
   * Requirement 4.2: Should return 404 when moving to non-existent stage
   */
  @Test
  @DisplayName("Should return 404 when moving to invalid stage")
  @WithMockUser(authorities = {"USER"})
  void testMoveToInvalidStage() throws Exception {
    // Given
    PipelineItem item = createPipelineItem(1L, newStage.getId());

    PipelineItemDtos.MoveToStageRequest request = new PipelineItemDtos.MoveToStageRequest(
        99999L
    );

    // When & Then
    mockMvc.perform(put("/api/v1/pipeline/items/{id}/stage", item.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  /**
   * Test move non-existent pipeline item.
   * Requirement 4.2: Should return 404 when pipeline item doesn't exist
   */
  @Test
  @DisplayName("Should return 404 when moving non-existent pipeline item")
  @WithMockUser(authorities = {"USER"})
  void testMoveNonExistentItem() throws Exception {
    // Given
    PipelineItemDtos.MoveToStageRequest request = new PipelineItemDtos.MoveToStageRequest(
        contactedStage.getId()
    );

    // When & Then
    mockMvc.perform(put("/api/v1/pipeline/items/{id}/stage", 99999L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  /**
   * Test pagination of pipeline items.
   * Requirement 4.1: Pipeline items should support pagination
   */
  @Test
  @DisplayName("Should paginate pipeline items")
  @WithMockUser(authorities = {"USER"})
  void testPaginatePipelineItems() throws Exception {
    // Given - Create 25 pipeline items
    for (long i = 1; i <= 25; i++) {
      createPipelineItem(i, newStage.getId());
    }

    // When & Then - Request first page with size 10
    mockMvc.perform(get("/api/v1/pipeline/items")
            .param("page", "0")
            .param("size", "10")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(10)))
        .andExpect(jsonPath("$.totalElements", is(25)))
        .andExpect(jsonPath("$.totalPages", is(3)))
        .andExpect(jsonPath("$.number", is(0)))
        .andExpect(jsonPath("$.size", is(10)));

    // Request second page
    mockMvc.perform(get("/api/v1/pipeline/items")
            .param("page", "1")
            .param("size", "10")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(10)))
        .andExpect(jsonPath("$.number", is(1)));

    // Request last page
    mockMvc.perform(get("/api/v1/pipeline/items")
            .param("page", "2")
            .param("size", "10")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(5)))
        .andExpect(jsonPath("$.number", is(2)));
  }

  // Helper methods

  private PipelineStage createStage(String name, int displayOrder, boolean isFinalStage) {
    PipelineStage stage = new PipelineStage(name, displayOrder);
    stage.setIsActive(true);
    stage.setIsFinalStage(isFinalStage);
    stage.setCreatedBy("test-user");
    stage.setUpdatedBy("test-user");
    return stageRepository.save(stage);
  }

  private PipelineItem createPipelineItem(Long leadId, Long stageId) {
    PipelineItem item = new PipelineItem();
    item.setLeadId(leadId);
    item.setStageId(stageId);
    item.setEnteredStageAt(LocalDateTime.now());
    item.setCreatedBy("test-user");
    item.setUpdatedBy("test-user");
    return itemRepository.save(item);
  }
}
