package com.iqscaffold.pipelineservice.pipeline;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PipelineStageRepository extends JpaRepository<PipelineStage, Long> {

  List<PipelineStage> findByIsActiveTrueOrderByDisplayOrderAsc();

  List<PipelineStage> findAllByOrderByDisplayOrderAsc();

  boolean existsByName(String name);
}
