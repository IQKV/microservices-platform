package com.iqscaffold.billingservice.config;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for cache management operations.
 * Restricted to admin users only.
 */
@RestController
@RequestMapping("/api/v1/admin/cache")
@Tag(name = "Cache Management", description = "APIs for managing Hibernate Second Level Cache")
public class CacheManagementRestResource {

  private final CacheManagementService cacheManagementService;

  public CacheManagementRestResource(final CacheManagementService cacheManagementService) {
    this.cacheManagementService = cacheManagementService;
  }

  @GetMapping("/statistics")
  @Operation(summary = "Get cache statistics", description = "Retrieve overall cache statistics")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, Object>> getCacheStatistics() {
    return ResponseEntity.ok(cacheManagementService.getCacheStatistics());
  }

  @DeleteMapping("/evict-all")
  @Operation(summary = "Evict all caches", description = "Clear all second level caches")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> evictAllCaches() {
    cacheManagementService.evictAllCaches();
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/evict-queries")
  @Operation(summary = "Evict query caches", description = "Clear all query caches")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> evictQueryCaches() {
    cacheManagementService.evictQueryCaches();
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/statistics/enable")
  @Operation(summary = "Enable statistics", description = "Enable cache statistics collection")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> enableStatistics() {
    cacheManagementService.setStatisticsEnabled(true);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/statistics/disable")
  @Operation(summary = "Disable statistics", description = "Disable cache statistics collection")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> disableStatistics() {
    cacheManagementService.setStatisticsEnabled(false);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/statistics")
  @Operation(summary = "Clear statistics", description = "Clear all cache statistics")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> clearStatistics() {
    cacheManagementService.clearStatistics();
    return ResponseEntity.noContent().build();
  }
}
