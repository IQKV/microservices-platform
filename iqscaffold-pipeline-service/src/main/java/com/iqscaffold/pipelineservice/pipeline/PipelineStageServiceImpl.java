package com.iqscaffold.pipelineservice.pipeline;

import com.iqscaffold.pipelineservice.shared.exception.BusinessException;
import com.iqscaffold.pipelineservice.shared.exception.ConflictException;
import com.iqscaffold.pipelineservice.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of PipelineStageService.
 * <p>
 * Provides business logic for managing pipeline stages including:
 * <ul>
 *   <li>Creating new stages</li>
 *   <li>Updating existing stages</li>
 *   <li>Deleting stages (with validation)</li>
 *   <li>Reordering stages</li>
 * </ul>
 */
@Service
@Transactional
public class PipelineStageServiceImpl implements PipelineStageService {

  private static final Logger logger = LoggerFactory.getLogger(PipelineStageServiceImpl.class);

  private final PipelineStageRepository stageRepository;
  private final PipelineItemRepository itemRepository;

  public PipelineStageServiceImpl(
      final PipelineStageRepository stageRepository,
      final PipelineItemRepository itemRepository) {
    this.stageRepository = stageRepository;
    this.itemRepository = itemRepository;
  }

  @Override
  @CacheEvict(value = "pipelineStages", allEntries = true)
  public PipelineStage createStage(final PipelineStage stage) {
    logger.debug("Creating new pipeline stage: {}", stage.getName());

    // Check for duplicate name
    if (stageRepository.existsByName(stage.getName())) {
      throw new ConflictException("Pipeline stage", "name", stage.getName());
    }

    // If no display order specified, set it to the end
    if (stage.getDisplayOrder() == null) {
      List<PipelineStage> allStages = stageRepository.findAllByOrderByDisplayOrderAsc();
      int maxOrder = allStages.stream()
          .mapToInt(PipelineStage::getDisplayOrder)
          .max()
          .orElse(-1);
      stage.setDisplayOrder(maxOrder + 1);
    }

    PipelineStage saved = stageRepository.save(stage);
    logger.info("Created pipeline stage with ID: {}", saved.getId());
    return saved;
  }

  @Override
  @Transactional(readOnly = true)
  @Cacheable(value = "pipelineStages", key = "#id")
  public Optional<PipelineStage> getStageById(final Long id) {
    logger.debug("Fetching pipeline stage by ID: {}", id);
    return stageRepository.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  @Cacheable(value = "pipelineStages", key = "'all'")
  public List<PipelineStage> getAllStages() {
    logger.debug("Fetching all pipeline stages");
    return stageRepository.findAllByOrderByDisplayOrderAsc();
  }

  @Override
  @Transactional(readOnly = true)
  @Cacheable(value = "pipelineStages", key = "'active'")
  public List<PipelineStage> getActiveStages() {
    logger.debug("Fetching active pipeline stages");
    return stageRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
  }

  @Override
  @CacheEvict(value = "pipelineStages", allEntries = true)
  public PipelineStage updateStage(final Long id, final PipelineStage updatedStage) {
    logger.debug("Updating pipeline stage with ID: {}", id);

    PipelineStage existingStage = stageRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Pipeline stage", "id", id));

    // Check for duplicate name (excluding current stage)
    if (!existingStage.getName().equals(updatedStage.getName()) &&
        stageRepository.existsByName(updatedStage.getName())) {
      throw new ConflictException("Pipeline stage", "name", updatedStage.getName());
    }

    // Update fields
    existingStage.setName(updatedStage.getName());
    existingStage.setDescription(updatedStage.getDescription());
    existingStage.setIsActive(updatedStage.getIsActive());
    existingStage.setIsFinalStage(updatedStage.getIsFinalStage());
    existingStage.setColorCode(updatedStage.getColorCode());
    existingStage.setUpdatedBy(updatedStage.getUpdatedBy());

    PipelineStage saved = stageRepository.save(existingStage);
    logger.info("Updated pipeline stage with ID: {}", saved.getId());
    return saved;
  }

  @Override
  @CacheEvict(value = "pipelineStages", allEntries = true)
  public void deleteStage(final Long id) {
    logger.debug("Deleting pipeline stage with ID: {}", id);

    PipelineStage stage = stageRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Pipeline stage", "id", id));

    // Check if any pipeline items are in this stage
    long itemCount = itemRepository.countByStageId(id);
    if (itemCount > 0) {
      throw new BusinessException(
          String.format("Cannot delete stage '%s' because %d lead(s) are currently in this stage. " +
              "Please move the leads to another stage first.", stage.getName(), itemCount));
    }

    stageRepository.delete(stage);
    logger.info("Deleted pipeline stage with ID: {}", id);
  }

  @Override
  @CacheEvict(value = "pipelineStages", allEntries = true)
  public PipelineStage updateStageOrder(final Long id, final Integer newOrder) {
    logger.debug("Updating order for pipeline stage with ID: {} to {}", id, newOrder);

    PipelineStage stage = stageRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Pipeline stage", "id", id));

    int oldOrder = stage.getDisplayOrder();
    if (oldOrder == newOrder) {
      return stage;
    }

    // Get all stages ordered by display order
    List<PipelineStage> allStages = stageRepository.findAllByOrderByDisplayOrderAsc();

    // Remove the stage being reordered
    allStages.removeIf(s -> s.getId().equals(id));

    // Insert at new position
    if (newOrder >= allStages.size()) {
      allStages.add(stage);
    } else {
      allStages.add(newOrder, stage);
    }

    // Update display orders for all stages
    for (int i = 0; i < allStages.size(); i++) {
      allStages.get(i).setDisplayOrder(i);
    }

    stageRepository.saveAll(allStages);
    logger.info("Updated order for pipeline stage with ID: {} from {} to {}", id, oldOrder, newOrder);

    return stage;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByName(final String name) {
    return stageRepository.existsByName(name);
  }
}
