package com.iqscaffold.pipelineservice.pipeline;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqscaffold.pipelineservice.pipeline.dto.PipelineStageDtos;
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

/**
 * Integration tests for Pipeline Stage management operations.
 * Tests Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PipelineStageRestResourceIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private PipelineStageRepository stageRepository;

  @Autowired
  private PipelineItemRepository itemRepository;

  @BeforeEach
  void setUp() {
    // Clean up data before each test
    itemRepository.deleteAll();
    stageRepository.deleteAll();
  }

  /**
   * Test list stages - verify default stages exist.
   * Requirement 3.1: THE CRM_System SHALL provide default pipeline stages:
   * New, Contacted, Qualified, Proposal, Won, Lost
   */
  @Test
  @DisplayName("Should list all pipeline stages ordered by display order")
  @WithMockUser(authorities = {"USER"})
  void testListStages() throws Exception {
    // Given - Create default stages
    createDefaultStages();

    // When & Then
    mockMvc.perform(get("/api/v1/pipeline/stages")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(6))))
        .andExpect(jsonPath("$[0].name", is("New")))
        .andExpect(jsonPath("$[0].displayOrder", is(0)))
        .andExpect(jsonPath("$[1].name", is("Contacted")))
        .andExpect(jsonPath("$[1].displayOrder", is(1)))
        .andExpect(jsonPath("$[2].name", is("Qualified")))
        .andExpect(jsonPath("$[2].displayOrder", is(2)))
        .andExpect(jsonPath("$[3].name", is("Proposal")))
        .andExpect(jsonPath("$[3].displayOrder", is(3)))
        .andExpect(jsonPath("$[4].name", is("Won")))
        .andExpect(jsonPath("$[4].displayOrder", is(4)))
        .andExpect(jsonPath("$[5].name", is("Lost")))
        .andExpect(jsonPath("$[5].displayOrder", is(5)));
  }

  /**
   * Test create custom stage.
   * Requirement 3.2: WHEN a user creates a custom pipeline stage with a name and order,
   * THE CRM_System SHALL persist the stage with tenant isolation
   */
  @Test
  @DisplayName("Should create custom pipeline stage")
  @WithMockUser(authorities = {"ADMIN"})
  void testCreateCustomStage() throws Exception {
    // Given
    PipelineStageDtos.CreateStageRequest request = new PipelineStageDtos.CreateStageRequest(
        "Negotiation",
        "Negotiating terms and pricing",
        3,
        false,
        "#FFA500"
    );

    // When & Then
    mockMvc.perform(post("/api/v1/pipeline/stages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.name", is("Negotiation")))
        .andExpect(jsonPath("$.description", is("Negotiating terms and pricing")))
        .andExpect(jsonPath("$.displayOrder", is(3)))
        .andExpect(jsonPath("$.isActive", is(true)))
        .andExpect(jsonPath("$.isFinalStage", is(false)))
        .andExpect(jsonPath("$.colorCode", is("#FFA500")))
        .andExpect(jsonPath("$.createdAt", notNullValue()))
        .andExpect(jsonPath("$.updatedAt", notNullValue()))
        .andExpect(jsonPath("$.createdBy", notNullValue()))
        .andExpect(jsonPath("$.updatedBy", notNullValue()));
  }

  /**
   * Test create stage with duplicate name.
   * Requirement 3.2: Should reject duplicate stage names
   */
  @Test
  @DisplayName("Should return 409 when creating stage with duplicate name")
  @WithMockUser(authorities = {"ADMIN"})
  void testCreateStageWithDuplicateName() throws Exception {
    // Given - Create a stage first
    PipelineStage existingStage = new PipelineStage("Discovery", 0);
    existingStage.setIsActive(true);
    existingStage.setIsFinalStage(false);
    existingStage.setCreatedBy("test-user");
    existingStage.setUpdatedBy("test-user");
    stageRepository.save(existingStage);

    // Try to create another stage with the same name
    PipelineStageDtos.CreateStageRequest request = new PipelineStageDtos.CreateStageRequest(
        "Discovery",
        "Another discovery stage",
        1,
        false,
        "#FF0000"
    );

    // When & Then
    mockMvc.perform(post("/api/v1/pipeline/stages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }

  /**
   * Test create stage - authorization check.
   * Requirement 3.2: Only ADMIN or SUPER_ADMIN can create stages
   */
  @Test
  @DisplayName("Should return 403 when USER tries to create stage")
  @WithMockUser(authorities = {"USER"})
  void testCreateStageForbiddenForUser() throws Exception {
    // Given
    PipelineStageDtos.CreateStageRequest request = new PipelineStageDtos.CreateStageRequest(
        "Test Stage",
        "Test description",
        0,
        false,
        "#000000"
    );

    // When & Then
    mockMvc.perform(post("/api/v1/pipeline/stages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  /**
   * Test update stage.
   * Requirement 3.3: WHEN a user updates a pipeline stage's name or order,
   * THE CRM_System SHALL save the changes and maintain stage ordering
   */
  @Test
  @DisplayName("Should update pipeline stage")
  @WithMockUser(authorities = {"ADMIN"})
  void testUpdateStage() throws Exception {
    // Given - Create a stage first
    PipelineStage stage = new PipelineStage("Initial Name", 0);
    stage.setDescription("Initial description");
    stage.setIsActive(true);
    stage.setIsFinalStage(false);
    stage.setColorCode("#000000");
    stage.setCreatedBy("test-user");
    stage.setUpdatedBy("test-user");
    PipelineStage savedStage = stageRepository.save(stage);

    // Update request
    PipelineStageDtos.UpdateStageRequest updateRequest = new PipelineStageDtos.UpdateStageRequest(
        "Updated Name",
        "Updated description",
        true,
        false,
        "#FFFFFF"
    );

    // When & Then
    mockMvc.perform(put("/api/v1/pipeline/stages/{id}", savedStage.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(savedStage.getId().intValue())))
        .andExpect(jsonPath("$.name", is("Updated Name")))
        .andExpect(jsonPath("$.description", is("Updated description")))
        .andExpect(jsonPath("$.isActive", is(true)))
        .andExpect(jsonPath("$.isFinalStage", is(false)))
        .andExpect(jsonPath("$.colorCode", is("#FFFFFF")))
        .andExpect(jsonPath("$.updatedAt", notNullValue()));
  }

  /**
   * Test update stage - not found scenario.
   * Requirement 3.3: Should return 404 when updating non-existent stage
   */
  @Test
  @DisplayName("Should return 404 when updating non-existent stage")
  @WithMockUser(authorities = {"ADMIN"})
  void testUpdateStageNotFound() throws Exception {
    // Given
    PipelineStageDtos.UpdateStageRequest updateRequest = new PipelineStageDtos.UpdateStageRequest(
        "Test Stage",
        "Test description",
        true,
        false,
        "#000000"
    );

    // When & Then
    mockMvc.perform(put("/api/v1/pipeline/stages/{id}", 99999L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isNotFound());
  }

  /**
   * Test update stage - authorization check.
   * Requirement 3.3: Only ADMIN or SUPER_ADMIN can update stages
   */
  @Test
  @DisplayName("Should return 403 when USER tries to update stage")
  @WithMockUser(authorities = {"USER"})
  void testUpdateStageForbiddenForUser() throws Exception {
    // Given - Create a stage first
    PipelineStage stage = new PipelineStage("Test Stage", 0);
    stage.setIsActive(true);
    stage.setIsFinalStage(false);
    stage.setCreatedBy("test-user");
    stage.setUpdatedBy("test-user");
    PipelineStage savedStage = stageRepository.save(stage);

    // Update request
    PipelineStageDtos.UpdateStageRequest updateRequest = new PipelineStageDtos.UpdateStageRequest(
        "Updated Name",
        "Updated description",
        true,
        false,
        "#FFFFFF"
    );

    // When & Then
    mockMvc.perform(put("/api/v1/pipeline/stages/{id}", savedStage.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isForbidden());
  }

  /**
   * Test delete stage with validation.
   * Requirement 3.4: WHEN a user deletes a pipeline stage,
   * THE CRM_System SHALL prevent deletion if leads exist in that stage
   */
  @Test
  @DisplayName("Should delete stage when no leads are in it")
  @WithMockUser(authorities = {"ADMIN"})
  void testDeleteStageWithNoLeads() throws Exception {
    // Given - Create a stage with no leads
    PipelineStage stage = new PipelineStage("Empty Stage", 0);
    stage.setIsActive(true);
    stage.setIsFinalStage(false);
    stage.setCreatedBy("test-user");
    stage.setUpdatedBy("test-user");
    PipelineStage savedStage = stageRepository.save(stage);

    // When & Then
    mockMvc.perform(delete("/api/v1/pipeline/stages/{id}", savedStage.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    // Verify stage is deleted
    mockMvc.perform(get("/api/v1/pipeline/stages")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == " + savedStage.getId() + ")]").doesNotExist());
  }

  /**
   * Test delete stage with leads in it.
   * Requirement 3.4: Should prevent deletion if leads exist in the stage
   */
  @Test
  @DisplayName("Should return 400 when deleting stage with leads")
  @WithMockUser(authorities = {"ADMIN"})
  void testDeleteStageWithLeads() throws Exception {
    // Given - Create a stage with a lead in it
    PipelineStage stage = new PipelineStage("Active Stage", 0);
    stage.setIsActive(true);
    stage.setIsFinalStage(false);
    stage.setCreatedBy("test-user");
    stage.setUpdatedBy("test-user");
    PipelineStage savedStage = stageRepository.save(stage);

    // Create a pipeline item in this stage
    PipelineItem item = new PipelineItem();
    item.setLeadId(1L);
    item.setStageId(savedStage.getId());
    item.setEnteredStageAt(LocalDateTime.now());
    item.setCreatedBy("test-user");
    item.setUpdatedBy("test-user");
    itemRepository.save(item);

    // When & Then
    mockMvc.perform(delete("/api/v1/pipeline/stages/{id}", savedStage.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  /**
   * Test delete stage - not found scenario.
   * Requirement 3.4: Should return 404 when deleting non-existent stage
   */
  @Test
  @DisplayName("Should return 404 when deleting non-existent stage")
  @WithMockUser(authorities = {"ADMIN"})
  void testDeleteStageNotFound() throws Exception {
    // When & Then
    mockMvc.perform(delete("/api/v1/pipeline/stages/{id}", 99999L)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  /**
   * Test delete stage - authorization check.
   * Requirement 3.4: Only ADMIN or SUPER_ADMIN can delete stages
   */
  @Test
  @DisplayName("Should return 403 when USER tries to delete stage")
  @WithMockUser(authorities = {"USER"})
  void testDeleteStageForbiddenForUser() throws Exception {
    // Given - Create a stage first
    PipelineStage stage = new PipelineStage("Test Stage", 0);
    stage.setIsActive(true);
    stage.setIsFinalStage(false);
    stage.setCreatedBy("test-user");
    stage.setUpdatedBy("test-user");
    PipelineStage savedStage = stageRepository.save(stage);

    // When & Then
    mockMvc.perform(delete("/api/v1/pipeline/stages/{id}", savedStage.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  /**
   * Test reorder stages.
   * Requirement 3.5: WHEN a user reorders pipeline stages,
   * THE CRM_System SHALL update the order values to maintain sequential ordering
   */
  @Test
  @DisplayName("Should reorder pipeline stages")
  @WithMockUser(authorities = {"ADMIN"})
  void testReorderStages() throws Exception {
    // Given - Create multiple stages
    createStage("Stage 1", 0);
    createStage("Stage 2", 1);
    createStage("Stage 3", 2);
    PipelineStage stage4 = createStage("Stage 4", 3);

    // When & Then - Move stage4 to position 1 (between stage1 and stage2)
    mockMvc.perform(put("/api/v1/pipeline/stages/{id}/order", stage4.getId())
            .param("newOrder", "1")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(stage4.getId().intValue())))
        .andExpect(jsonPath("$.displayOrder", is(1)));

    // Verify the new order
    mockMvc.perform(get("/api/v1/pipeline/stages")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(4)))
        .andExpect(jsonPath("$[0].name", is("Stage 1")))
        .andExpect(jsonPath("$[0].displayOrder", is(0)))
        .andExpect(jsonPath("$[1].name", is("Stage 4")))
        .andExpect(jsonPath("$[1].displayOrder", is(1)))
        .andExpect(jsonPath("$[2].name", is("Stage 2")))
        .andExpect(jsonPath("$[2].displayOrder", is(2)))
        .andExpect(jsonPath("$[3].name", is("Stage 3")))
        .andExpect(jsonPath("$[3].displayOrder", is(3)));
  }

  /**
   * Test reorder stages - move to end.
   * Requirement 3.5: Should handle moving a stage to the end of the list
   */
  @Test
  @DisplayName("Should move stage to end of list")
  @WithMockUser(authorities = {"ADMIN"})
  void testReorderStageToEnd() throws Exception {
    // Given - Create multiple stages
    PipelineStage stage1 = createStage("Stage 1", 0);
    createStage("Stage 2", 1);
    createStage("Stage 3", 2);

    // When & Then - Move stage1 to the end
    mockMvc.perform(put("/api/v1/pipeline/stages/{id}/order", stage1.getId())
            .param("newOrder", "2")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    // Verify the new order
    mockMvc.perform(get("/api/v1/pipeline/stages")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].name", is("Stage 2")))
        .andExpect(jsonPath("$[0].displayOrder", is(0)))
        .andExpect(jsonPath("$[1].name", is("Stage 3")))
        .andExpect(jsonPath("$[1].displayOrder", is(1)))
        .andExpect(jsonPath("$[2].name", is("Stage 1")))
        .andExpect(jsonPath("$[2].displayOrder", is(2)));
  }

  /**
   * Test reorder stages - not found scenario.
   * Requirement 3.5: Should return 404 when reordering non-existent stage
   */
  @Test
  @DisplayName("Should return 404 when reordering non-existent stage")
  @WithMockUser(authorities = {"ADMIN"})
  void testReorderStageNotFound() throws Exception {
    // When & Then
    mockMvc.perform(put("/api/v1/pipeline/stages/{id}/order", 99999L)
            .param("newOrder", "0")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  /**
   * Test reorder stages - authorization check.
   * Requirement 3.5: Only ADMIN or SUPER_ADMIN can reorder stages
   */
  @Test
  @DisplayName("Should return 403 when USER tries to reorder stage")
  @WithMockUser(authorities = {"USER"})
  void testReorderStageForbiddenForUser() throws Exception {
    // Given - Create a stage first
    PipelineStage stage = createStage("Test Stage", 0);

    // When & Then
    mockMvc.perform(put("/api/v1/pipeline/stages/{id}/order", stage.getId())
            .param("newOrder", "1")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  /**
   * Test retrieve stages sorted by order.
   * Requirement 3.6: WHEN a user retrieves pipeline stages,
   * THE CRM_System SHALL return them sorted by order value
   */
  @Test
  @DisplayName("Should return stages sorted by display order")
  @WithMockUser(authorities = {"USER"})
  void testStagesSortedByOrder() throws Exception {
    // Given - Create stages in random order
    createStage("Third", 2);
    createStage("First", 0);
    createStage("Fifth", 4);
    createStage("Second", 1);
    createStage("Fourth", 3);

    // When & Then
    mockMvc.perform(get("/api/v1/pipeline/stages")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(5)))
        .andExpect(jsonPath("$[0].name", is("First")))
        .andExpect(jsonPath("$[0].displayOrder", is(0)))
        .andExpect(jsonPath("$[1].name", is("Second")))
        .andExpect(jsonPath("$[1].displayOrder", is(1)))
        .andExpect(jsonPath("$[2].name", is("Third")))
        .andExpect(jsonPath("$[2].displayOrder", is(2)))
        .andExpect(jsonPath("$[3].name", is("Fourth")))
        .andExpect(jsonPath("$[3].displayOrder", is(3)))
        .andExpect(jsonPath("$[4].name", is("Fifth")))
        .andExpect(jsonPath("$[4].displayOrder", is(4)));
  }

  // Helper methods

  private void createDefaultStages() {
    createStage("New", 0);
    createStage("Contacted", 1);
    createStage("Qualified", 2);
    createStage("Proposal", 3);
    createStage("Won", 4);
    createStage("Lost", 5);
  }

  private PipelineStage createStage(String name, int displayOrder) {
    PipelineStage stage = new PipelineStage(name, displayOrder);
    stage.setIsActive(true);
    stage.setIsFinalStage(false);
    stage.setCreatedBy("test-user");
    stage.setUpdatedBy("test-user");
    return stageRepository.save(stage);
  }
}
