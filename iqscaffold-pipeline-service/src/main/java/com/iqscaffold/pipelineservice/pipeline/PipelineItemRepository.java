package com.iqscaffold.pipelineservice.pipeline;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PipelineItemRepository extends JpaRepository<PipelineItem, Long> {

  Optional<PipelineItem> findByLeadId(Long leadId);

  Page<PipelineItem> findByStageId(Long stageId, Pageable pageable);

  List<PipelineItem> findByStageId(Long stageId);

  long countByStageId(Long stageId);

  boolean existsByLeadId(Long leadId);
}
