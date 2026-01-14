package com.iqscaffold.pipelineservice.pipeline;

import java.util.List;
import java.util.Optional;

public interface PipelineStageService {

  PipelineStage createStage(PipelineStage stage);

  Optional<PipelineStage> getStageById(Long id);

  List<PipelineStage> getAllStages();

  List<PipelineStage> getActiveStages();

  PipelineStage updateStage(Long id, PipelineStage stage);

  void deleteStage(Long id);

  PipelineStage updateStageOrder(Long id, Integer newOrder);

  boolean existsByName(String name);
}
