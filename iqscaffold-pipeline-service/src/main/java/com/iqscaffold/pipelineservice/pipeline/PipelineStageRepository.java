package com.iqscaffold.pipelineservice.pipeline;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PipelineStageRepository extends JpaRepository<PipelineStage, Long> {

  List<PipelineStage> findByIsActiveTrueOrderByDisplayOrderAsc();

  List<PipelineStage> findAllByOrderByDisplayOrderAsc();

  boolean existsByName(String name);

  boolean existsByNameAndIdNot(String name, Long id);
}
