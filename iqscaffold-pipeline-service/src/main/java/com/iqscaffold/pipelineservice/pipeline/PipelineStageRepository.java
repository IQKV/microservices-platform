package com.iqscaffold.pipelineservice.pipeline;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PipelineStageRepository extends JpaRepository<PipelineStage, Long> {

  List<PipelineStage> findByIsActiveTrueOrderByDisplayOrderAsc();

  @EntityGraph("PipelineStage.withItems")
  List<PipelineStage> findWithItemsByIsActiveTrueOrderByDisplayOrderAsc();

  List<PipelineStage> findAllByOrderByDisplayOrderAsc();

  @EntityGraph("PipelineStage.withItems")
  List<PipelineStage> findWithItemsAllByOrderByDisplayOrderAsc();

  @EntityGraph("PipelineStage.withItems")
  Optional<PipelineStage> findWithItemsById(Long id);

  @EntityGraph("PipelineStage.basic")
  Optional<PipelineStage> findBasicById(Long id);

  boolean existsByName(String name);

  boolean existsByNameAndIdNot(String name, Long id);
}
