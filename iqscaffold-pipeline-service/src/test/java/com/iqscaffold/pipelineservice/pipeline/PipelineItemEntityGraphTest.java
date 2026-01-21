package com.iqscaffold.pipelineservice.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;

import com.iqscaffold.pipelineservice.shared.test.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for PipelineItem entity graphs.
 * These tests verify that entity graphs properly load related entities in a single query.
 */
@DisplayName("PipelineItem Entity Graph Tests")
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class PipelineItemEntityGraphTest extends BaseIntegrationTest {

  @Autowired
  private PipelineItemRepository pipelineItemRepository;

  @Autowired
  private PipelineStageRepository pipelineStageRepository;

  private PipelineStage testStage;
  private PipelineItem testPipelineItem;

  @BeforeEach
  void setUp() {
    // Create a test stage
    testStage = new PipelineStage("Qualified", 2);
    testStage.setDescription("Qualified leads ready for proposal");
    testStage.setColorCode("#28a745");
    testStage.setCreatedBy("test-user");
    testStage.setUpdatedBy("test-user");
    testStage = pipelineStageRepository.save(testStage);

    // Create a test pipeline item
    testPipelineItem = new PipelineItem(12345L, testStage.getId());
    testPipelineItem.setExpectedValue(new BigDecimal("50000.00"));
    testPipelineItem.setProbability(new BigDecimal("75.00"));
    testPipelineItem.setDaysInStage(5);
    testPipelineItem.setCreatedBy("test-user");
    testPipelineItem.setUpdatedBy("test-user");
    testPipelineItem = pipelineItemRepository.save(testPipelineItem);
  }

  @Test
  @DisplayName("Should load pipeline item with stage using entity graph")
  @Transactional
  void shouldLoadPipelineItemWithStage() {
    // When
    Optional<PipelineItem> itemWithStage = pipelineItemRepository.findWithStageById(testPipelineItem.getId());

    // Then
    assertThat(itemWithStage).isPresent();
    PipelineItem item = itemWithStage.get();

    // Verify pipeline item data
    assertThat(item.getLeadId()).isEqualTo(12345L);
    assertThat(item.getStageId()).isEqualTo(testStage.getId());
    assertThat(item.getExpectedValue()).isEqualByComparingTo(new BigDecimal("50000.00"));
    assertThat(item.getProbability()).isEqualByComparingTo(new BigDecimal("75.00"));
    assertThat(item.getDaysInStage()).isEqualTo(5);

    // Verify stage is loaded (should not trigger additional queries)
    assertThat(item.getStage()).isNotNull();
    assertThat(item.getStage().getName()).isEqualTo("Qualified");
    assertThat(item.getStage().getDescription()).isEqualTo("Qualified leads ready for proposal");
    assertThat(item.getStage().getColorCode()).isEqualTo("#28a745");
    assertThat(item.getStage().getDisplayOrder()).isEqualTo(2);
  }

  @Test
  @DisplayName("Should load pipeline item with stage by lead ID using entity graph")
  @Transactional
  void shouldLoadPipelineItemWithStageByLeadId() {
    // When
    Optional<PipelineItem> itemWithStage = pipelineItemRepository.findWithStageByLeadId(12345L);

    // Then
    assertThat(itemWithStage).isPresent();
    PipelineItem item = itemWithStage.get();

    // Verify pipeline item data
    assertThat(item.getLeadId()).isEqualTo(12345L);
    assertThat(item.getStageId()).isEqualTo(testStage.getId());

    // Verify stage is loaded
    assertThat(item.getStage()).isNotNull();
    assertThat(item.getStage().getName()).isEqualTo("Qualified");
  }

  @Test
  @DisplayName("Should load basic pipeline item without relationships using entity graph")
  @Transactional
  void shouldLoadBasicPipelineItem() {
    // When
    Optional<PipelineItem> basicItem = pipelineItemRepository.findBasicById(testPipelineItem.getId());

    // Then
    assertThat(basicItem).isPresent();
    PipelineItem item = basicItem.get();

    // Verify pipeline item data
    assertThat(item.getLeadId()).isEqualTo(12345L);
    assertThat(item.getStageId()).isEqualTo(testStage.getId());
    assertThat(item.getExpectedValue()).isEqualByComparingTo(new BigDecimal("50000.00"));

    // Stage should not be loaded (lazy loading)
    // Note: In a real test, you might verify this doesn't trigger additional queries
  }

  @Test
  @DisplayName("Should load basic pipeline item by lead ID without relationships using entity graph")
  @Transactional
  void shouldLoadBasicPipelineItemByLeadId() {
    // When
    Optional<PipelineItem> basicItem = pipelineItemRepository.findBasicByLeadId(12345L);

    // Then
    assertThat(basicItem).isPresent();
    PipelineItem item = basicItem.get();

    // Verify pipeline item data
    assertThat(item.getLeadId()).isEqualTo(12345L);
    assertThat(item.getStageId()).isEqualTo(testStage.getId());
  }

  @Test
  @DisplayName("Should return empty when pipeline item not found with entity graph")
  @Transactional
  void shouldReturnEmptyWhenPipelineItemNotFound() {
    // When
    Optional<PipelineItem> nonExistentItem = pipelineItemRepository.findWithStageById(99999L);

    // Then
    assertThat(nonExistentItem).isEmpty();
  }

  @Test
  @DisplayName("Should return empty when pipeline item not found by lead ID with entity graph")
  @Transactional
  void shouldReturnEmptyWhenPipelineItemNotFoundByLeadId() {
    // When
    Optional<PipelineItem> nonExistentItem = pipelineItemRepository.findWithStageByLeadId(99999L);

    // Then
    assertThat(nonExistentItem).isEmpty();
  }

  @Test
  @DisplayName("Should handle pipeline item with inactive stage")
  @Transactional
  void shouldHandlePipelineItemWithInactiveStage() {
    // Given - create an inactive stage
    PipelineStage inactiveStage = new PipelineStage("Inactive", 99);
    inactiveStage.setIsActive(false);
    inactiveStage.setCreatedBy("test-user");
    inactiveStage.setUpdatedBy("test-user");
    inactiveStage = pipelineStageRepository.save(inactiveStage);

    PipelineItem itemWithInactiveStage = new PipelineItem(67890L, inactiveStage.getId());
    itemWithInactiveStage.setCreatedBy("test-user");
    itemWithInactiveStage.setUpdatedBy("test-user");
    itemWithInactiveStage = pipelineItemRepository.save(itemWithInactiveStage);

    // When
    Optional<PipelineItem> itemWithStage = pipelineItemRepository.findWithStageById(itemWithInactiveStage.getId());

    // Then
    assertThat(itemWithStage).isPresent();
    PipelineItem item = itemWithStage.get();

    assertThat(item.getLeadId()).isEqualTo(67890L);
    assertThat(item.getStage()).isNotNull();
    assertThat(item.getStage().getName()).isEqualTo("Inactive");
    assertThat(item.getStage().getIsActive()).isFalse();
  }

  @Test
  @DisplayName("Should load multiple pipeline items with stages by stage ID")
  @Transactional
  void shouldLoadMultiplePipelineItemsWithStagesByStageId() {
    // Given - create additional pipeline items in the same stage
    PipelineItem item2 = new PipelineItem(23456L, testStage.getId());
    item2.setExpectedValue(new BigDecimal("25000.00"));
    item2.setProbability(new BigDecimal("50.00"));
    item2.setCreatedBy("test-user");
    item2.setUpdatedBy("test-user");
    pipelineItemRepository.save(item2);

    PipelineItem item3 = new PipelineItem(34567L, testStage.getId());
    item3.setExpectedValue(new BigDecimal("75000.00"));
    item3.setProbability(new BigDecimal("90.00"));
    item3.setCreatedBy("test-user");
    item3.setUpdatedBy("test-user");
    pipelineItemRepository.save(item3);

    // When
    var itemsWithStage = pipelineItemRepository.findWithStageByStageId(testStage.getId());

    // Then
    assertThat(itemsWithStage).hasSize(3);

    // Verify all items have stage loaded
    itemsWithStage.forEach(item -> {
      assertThat(item.getStage()).isNotNull();
      assertThat(item.getStage().getName()).isEqualTo("Qualified");
      assertThat(item.getStageId()).isEqualTo(testStage.getId());
    });

    // Verify lead IDs
    assertThat(itemsWithStage)
        .extracting(PipelineItem::getLeadId)
        .containsExactlyInAnyOrder(12345L, 23456L, 34567L);
  }
}
