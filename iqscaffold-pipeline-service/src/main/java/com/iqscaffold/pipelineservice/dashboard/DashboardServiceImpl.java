package com.iqscaffold.pipelineservice.dashboard;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.iqscaffold.pipelineservice.config.IqScaffoldProperties;
import com.iqscaffold.pipelineservice.dashboard.dto.DashboardDtos;
import com.iqscaffold.pipelineservice.pipeline.PipelineItem;
import com.iqscaffold.pipelineservice.pipeline.PipelineItemRepository;
import com.iqscaffold.pipelineservice.pipeline.PipelineStage;
import com.iqscaffold.pipelineservice.pipeline.PipelineStageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Implementation of dashboard service for statistics and metrics.
 */
@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

  private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);
  private static final String LEAD_SERVICE_STATS_BY_SOURCE_PATH = "/api/v1/leads/stats/by-source";

  private final PipelineItemRepository pipelineItemRepository;
  private final PipelineStageRepository stageRepository;
  private final WebClient webClient;
  private final IqScaffoldProperties properties;

  public DashboardServiceImpl(
      final PipelineItemRepository pipelineItemRepository,
      final PipelineStageRepository stageRepository,
      final WebClient webClient,
      final IqScaffoldProperties properties) {
    this.pipelineItemRepository = pipelineItemRepository;
    this.stageRepository = stageRepository;
    this.webClient = webClient;
    this.properties = properties;
  }

  @Override
  public DashboardDtos.DashboardStatsResponse getDashboardStats(LocalDate startDate, LocalDate endDate) {
    log.debug("Getting dashboard stats for date range: {} to {}", startDate, endDate);

    // Get all pipeline items (date filtering would require additional query methods)
    List<PipelineItem> allItems = pipelineItemRepository.findAll();

    // Filter by date range if provided
    List<PipelineItem> filteredItems = filterByDateRange(allItems, startDate, endDate);

    // Get all stages for mapping
    List<PipelineStage> stages = stageRepository.findAllByOrderByDisplayOrderAsc();
    Map<Long, String> stageIdToName = stages.stream()
        .collect(Collectors.toMap(PipelineStage::getId, PipelineStage::getName));

    // Count leads by stage
    Map<String, Long> leadsByStage = filteredItems.stream()
        .collect(Collectors.groupingBy(
            item -> stageIdToName.getOrDefault(item.getStageId(), "Unknown"),
            Collectors.counting()
        ));

    // Get leads by source from Lead Service
    Map<String, Long> leadsBySource = getLeadsBySourceFromLeadService(startDate, endDate);

    // Calculate totals
    Long totalLeads = (long) filteredItems.size();
    Long wonLeads = leadsByStage.getOrDefault("Won", 0L);
    Long lostLeads = leadsByStage.getOrDefault("Lost", 0L);
    Long activeLeads = totalLeads - wonLeads - lostLeads;

    return new DashboardDtos.DashboardStatsResponse(
        leadsByStage,
        leadsBySource,
        totalLeads,
        activeLeads,
        wonLeads,
        lostLeads
    );
  }

  @Override
  public DashboardDtos.ConversionMetricsResponse getConversionMetrics(LocalDate startDate, LocalDate endDate) {
    log.debug("Getting conversion metrics for date range: {} to {}", startDate, endDate);

    // Get all pipeline items
    List<PipelineItem> allItems = pipelineItemRepository.findAll();

    // Filter by date range if provided
    List<PipelineItem> filteredItems = filterByDateRange(allItems, startDate, endDate);

    // Get Won stage
    PipelineStage wonStage = stageRepository.findAllByOrderByDisplayOrderAsc().stream()
        .filter(stage -> "Won".equals(stage.getName()))
        .findFirst()
        .orElse(null);

    // Calculate conversion rate
    Long totalLeads = (long) filteredItems.size();
    Long wonLeads = wonStage != null
        ? filteredItems.stream().filter(item -> item.getStageId().equals(wonStage.getId())).count()
        : 0L;

    Double conversionRate = totalLeads > 0
        ? (wonLeads.doubleValue() / totalLeads.doubleValue()) * 100.0
        : 0.0;

    // Calculate average time to convert (for Won leads only)
    List<PipelineItem> wonItems = wonStage != null
        ? filteredItems.stream()
            .filter(item -> item.getStageId().equals(wonStage.getId()) && item.getConvertedAt() != null)
            .toList()
        : List.of();

    Double averageTimeToConvert = wonItems.isEmpty() ? 0.0 : wonItems.stream()
        .mapToDouble(item -> ChronoUnit.DAYS.between(item.getCreatedAt(), item.getConvertedAt()))
        .average()
        .orElse(0.0);

    // Calculate stage velocity (average days in each stage)
    Map<String, Double> stageVelocity = calculateStageVelocity(filteredItems);

    return new DashboardDtos.ConversionMetricsResponse(
        conversionRate,
        averageTimeToConvert,
        stageVelocity
    );
  }

  /**
   * Filters pipeline items by date range based on creation date.
   */
  private List<PipelineItem> filterByDateRange(
      final List<PipelineItem> items,
      final LocalDate startDate,
      final LocalDate endDate) {
    if (startDate == null && endDate == null) {
      return items;
    }

    return items.stream()
        .filter(item -> {
          LocalDate createdDate = item.getCreatedAt().toLocalDate();
          boolean afterStart = startDate == null || !createdDate.isBefore(startDate);
          boolean beforeEnd = endDate == null || !createdDate.isAfter(endDate);
          return afterStart && beforeEnd;
        })
        .toList();
  }

  /**
   * Calculates average days spent in each stage.
   */
  private Map<String, Double> calculateStageVelocity(final List<PipelineItem> items) {
    List<PipelineStage> stages = stageRepository.findAllByOrderByDisplayOrderAsc();
    Map<Long, String> stageIdToName = stages.stream()
        .collect(Collectors.toMap(PipelineStage::getId, PipelineStage::getName));

    Map<String, List<Integer>> daysInStageByStage = new HashMap<>();

    for (final PipelineItem item : items) {
      String stageName = stageIdToName.getOrDefault(item.getStageId(), "Unknown");
      int daysInStage = item.getDaysInStage() != null ? item.getDaysInStage() : 0;

      daysInStageByStage.computeIfAbsent(stageName, k -> new java.util.ArrayList<>()).add(daysInStage);
    }

    return daysInStageByStage.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0)
        ));
  }

  /**
   * Calls Lead Service to get lead counts by source using WebClient.
   */
  private Map<String, Long> getLeadsBySourceFromLeadService(final LocalDate startDate, final LocalDate endDate) {
    try {
      String leadServiceUrl = properties.getServices().getLeadServiceUrl();

      log.debug("Calling Lead Service at: {}{}", leadServiceUrl, LEAD_SERVICE_STATS_BY_SOURCE_PATH);

      Map<String, Long> result = webClient.get()
          .uri(uriBuilder -> {
            uriBuilder.path(leadServiceUrl + LEAD_SERVICE_STATS_BY_SOURCE_PATH);
            
            if (startDate != null) {
              uriBuilder.queryParam("startDate", startDate.toString());
            }
            if (endDate != null) {
              uriBuilder.queryParam("endDate", endDate.toString());
            }
            
            return uriBuilder.build();
          })
          .retrieve()
          .bodyToMono(new ParameterizedTypeReference<Map<String, Long>>() {})
          .block();

      return result != null ? result : new HashMap<>();

    } catch (final WebClientResponseException e) {
      log.error("Failed to fetch leads by source from Lead Service: {} - {}", 
          e.getStatusCode(), e.getMessage());
      // Return empty map on failure - dashboard should still work with pipeline data
      return new HashMap<>();
    } catch (final Exception e) {
      log.error("Unexpected error calling Lead Service", e);
      return new HashMap<>();
    }
  }
}
