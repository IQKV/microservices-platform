package com.iqscaffold.pipelineservice.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.iqscaffold.pipelineservice.shared.test.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for PipelineStage entity graphs.
 * These tests verify that entity graphs properly load related entities in a single query.
 */
@DisplayName("PipelineStage Entity Graph Tests")
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class PipelineStageEntityGraphTest extends BaseIntegrationTest {

  @Autowired
  private PipelineStageRepository pipelineStageRepository;

  @Autowired
  private PipelineItemRepository pipelineItemRepository;

  private PipelineStage testStage;
  private PipelineStage inactiveStage;

  @BeforeEach
  void setUp() {
    // Create test stages
    testStage = new PipelineStage("Qualified", 2);
    testStage.setDescription("Qualified leads ready for proposal");
    testStage.setColorCode("#28a745");
    testStage.setCreatedBy("test-user");
    testStage.setUpdatedBy("test-user");
    testStage = pipelineStageRepository.save(testStage);

    inactiveStage = new PipelineStage("Inactive", 99);
    inactiveStage.setDescription("Inactive stage");
    inactiveStage.setIsActive(false);
    inactiveStage.setCreatedBy("test-user");
    inactiveStage.setUpdatedBy("test-user");
    inactiveStage = pipelineStageRepository.save(inactiveStage);

    // Create pipeline items for the test stage
    PipelineItem item1 = new PipelineItem(12345L, testStage.getId());
    item1.setExpectedValue(new BigDecimal("50000.00"));
    item1.setProbability(new BigDecimal("75.00"));
    item1.setCreatedBy("test-user");
    item1.setUpdatedBy("test-user");
    item1.setStage(testStage); // Set the relationship
    item1 = pipelineItemRepository.save(item1);

    PipelineItem item2 = new PipelineItem(23456L, testStage.getId());
    item2.setExpectedValue(new BigDecimal("25000.00"));
    item2.setProbability(new BigDecimal("50.00"));
    item2.setCreatedBy("test-user");
    item2.setUpdatedBy("test-user");
    item2.setStage(testStage); // Set the relationship
    item2 = pipelineItemRepository.save(item2);

    // Add items to the stage's collection
    testStage.getPipelineItems().add(item1);
    testStage.getPipelineItems().add(item2);

    // Create one item for inactive stage
    PipelineItem item3 = new PipelineItem(34567L, inactiveStage.getId());
    item3.setExpectedValue(new BigDecimal("10000.00"));
    item3.setCreatedBy("test-user");
    item3.setUpdatedBy("test-user");
    item3.setStage(inactiveStage); // Set the relationship
    item3 = pipelineItemRepository.save(item3);

    // Add item to the inactive stage's collection
    inactiveStage.getPipelineItems().add(item3);
  }

  @Test
  @DisplayName("Should load pipeline stage with items using entity graph")
  @Transactional
  void shouldLoadPipelineStageWithItems() {
    // When
    Optional<PipelineStage> stageWithItems = pipelineStageRepository.findWithItemsById(testStage.getId());

    // Then
    assertThat(stageWithItems).isPresent();
    PipelineStage stage = stageWithItems.get();

    // Verify stage data
    assertThat(stage.getName()).isEqualTo("Qualified");
    assertThat(stage.getDescription()).isEqualTo("Qualified leads ready for proposal");
    assertThat(stage.getColorCode()).isEqualTo("#28a745");
    assertThat(stage.getDisplayOrder()).isEqualTo(2);
    assertThat(stage.getIsActive()).isTrue();

    // Verify pipeline items are loaded (should not trigger additional queries)
    assertThat(stage.getPipelineItems()).hasSize(2);
    assertThat(stage.getPipelineItems())
        .extracting(PipelineItem::getLeadId)
        .containsExactlyInAnyOrder(12345L, 23456L);

    // Verify item details
    stage.getPipelineItems().forEach(item -> {
      assertThat(item.getStageId()).isEqualTo(testStage.getId());
      assertThat(item.getExpectedValue()).isNotNull();
    });
  }

  @Test
  @DisplayName("Should load basic pipeline stage without relationships using entity graph")
  @Transactional
  void shouldLoadBasicPipelineStage() {
    // When
    Optional<PipelineStage> basicStage = pipelineStageRepository.findBasicById(testStage.getId());

    // Then
    assertThat(basicStage).isPresent();
    PipelineStage stage = basicStage.get();

    // Verify stage data
    assertThat(stage.getName()).isEqualTo("Qualified");
    assertThat(stage.getDescription()).isEqualTo("Qualified leads ready for proposal");
    assertThat(stage.getColorCode()).isEqualTo("#28a745");

    // Pipeline items should not be loaded (lazy loading)
    // Note: In a real test, you might verify this doesn't trigger additional queries
  }

  @Test
  @DisplayName("Should load active pipeline stages with items using entity graph")
  @Transactional
  void shouldLoadActivePipelineStagesWithItems() {
    // When
    List<PipelineStage> activeStagesWithItems = pipelineStageRepository.findWithItemsByIsActiveTrueOrderByDisplayOrderAsc();

    // Then
    assertThat(activeStagesWithItems).hasSize(1);
    PipelineStage stage = activeStagesWithItems.get(0);

    // Verify it's the active stage
    assertThat(stage.getName()).isEqualTo("Qualified");
    assertThat(stage.getIsActive()).isTrue();

    // Verify items are loaded
    assertThat(stage.getPipelineItems()).hasSize(2);
    assertThat(stage.getPipelineItems())
        .extracting(PipelineItem::getLeadId)
        .containsExactlyInAnyOrder(12345L, 23456L);
  }

  @Test
  @DisplayName("Should load all pipeline stages with items using entity graph")
  @Transactional
  void shouldLoadAllPipelineStagesWithItems() {
    // When
    List<PipelineStage> allStagesWithItems = pipelineStageRepository.findWithItemsAllByOrderByDisplayOrderAsc();

    // Then
    assertThat(allStagesWithItems).hasSize(2);

    // Verify stages are ordered by display order
    assertThat(allStagesWithItems.get(0).getName()).isEqualTo("Qualified");
    assertThat(allStagesWithItems.get(0).getDisplayOrder()).isEqualTo(2);
    assertThat(allStagesWithItems.get(1).getName()).isEqualTo("Inactive");
    assertThat(allStagesWithItems.get(1).getDisplayOrder()).isEqualTo(99);

    // Verify items are loaded for each stage
    PipelineStage qualifiedStage = allStagesWithItems.get(0);
    assertThat(qualifiedStage.getPipelineItems()).hasSize(2);

    PipelineStage inactiveStageLoaded = allStagesWithItems.get(1);
    assertThat(inactiveStageLoaded.getPipelineItems()).hasSize(1);
    assertThat(inactiveStageLoaded.getPipelineItems().get(0).getLeadId()).isEqualTo(34567L);
  }

  @Test
  @DisplayName("Should return empty when pipeline stage not found with entity graph")
  @Transactional
  void shouldReturnEmptyWhenPipelineStageNotFound() {
    // When
    Optional<PipelineStage> nonExistentStage = pipelineStageRepository.findWithItemsById(99999L);

    // Then
    assertThat(nonExistentStage).isEmpty();
  }

  @Test
  @DisplayName("Should handle pipeline stage with no items")
  @Transactional
  void shouldHandlePipelineStageWithNoItems() {
    // Given - create a stage with no items
    PipelineStage emptyStage = new PipelineStage("Empty", 1);
    emptyStage.setDescription("Stage with no items");
    emptyStage.setCreatedBy("test-user");
    emptyStage.setUpdatedBy("test-user");
    emptyStage = pipelineStageRepository.save(emptyStage);

    // When
    Optional<PipelineStage> stageWithItems = pipelineStageRepository.findWithItemsById(emptyStage.getId());

    // Then
    assertThat(stageWithItems).isPresent();
    PipelineStage stage = stageWithItems.get();

    assertThat(stage.getName()).isEqualTo("Empty");
    assertThat(stage.getPipelineItems()).isEmpty();
  }

  @Test
  @DisplayName("Should verify pipeline items are properly associated with stage")
  @Transactional
  void shouldVerifyPipelineItemsAreProperlyAssociated() {
    // When
    Optional<PipelineStage> stageWithItems = pipelineStageRepository.findWithItemsById(testStage.getId());

    // Then
    assertThat(stageWithItems).isPresent();
    PipelineStage stage = stageWithItems.get();

    // Verify all items belong to this stage
    stage.getPipelineItems().forEach(item -> {
      assertThat(item.getStageId()).isEqualTo(testStage.getId());
      assertThat(item.getCreatedBy()).isEqualTo("test-user");
      assertThat(item.getUpdatedBy()).isEqualTo("test-user");
    });

    // Verify expected values are loaded
    List<BigDecimal> expectedValues = stage.getPipelineItems().stream()
        .map(PipelineItem::getExpectedValue)
        .toList();

    assertThat(expectedValues)
        .containsExactlyInAnyOrder(
            new BigDecimal("50000.00"),
            new BigDecimal("25000.00")
        );
  }
}
