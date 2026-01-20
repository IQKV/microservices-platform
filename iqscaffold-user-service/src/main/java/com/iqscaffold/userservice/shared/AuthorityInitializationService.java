package com.iqscaffold.userservice.shared;

import com.iqscaffold.userservice.config.PlatformConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for initializing system authorities on application startup.
 * 
 * <p>This service ensures that all authorities defined in platform configuration
 * are created in the database during application startup. This is essential for:
 * <ul>
 *   <li>Ensuring configurable authorities exist</li>
 *   <li>Maintaining consistency across environments</li>
 *   <li>Supporting authority-based access control from day one</li>
 *   <li>Preventing runtime errors when assigning authorities</li>
 * </ul>
 * 
 * <h3>Initialization Process</h3>
 * <ul>
 *   <li>Runs automatically on {@link ApplicationReadyEvent}</li>
 *   <li>Creates missing authorities with proper descriptions from configuration</li>
 *   <li>Skips authorities that already exist</li>
 *   <li>Logs all initialization activities</li>
 * </ul>
 * 
 * <h3>Configuration-Driven</h3>
 * <ul>
 *   <li>Authorities are defined in application.yml under iqscaffold.platform.authorities</li>
 *   <li>Supports custom authority definitions without code changes</li>
 *   <li>Fully extensible through configuration</li>
 * </ul>
 */
@Service
@Transactional
public class AuthorityInitializationService {

  private static final Logger logger = LoggerFactory.getLogger(AuthorityInitializationService.class);

  private final AuthorityRepository authorityRepository;
  private final PlatformConfigurationProperties platformConfig;

  public AuthorityInitializationService(final AuthorityRepository authorityRepository,
                                        final PlatformConfigurationProperties platformConfig) {
    this.authorityRepository = authorityRepository;
    this.platformConfig = platformConfig;
  }

  /**
   * Initialize all authorities defined in configuration on application startup.
   * This method is automatically called when the application is ready.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void initializeAuthorities() {
    logger.info("Initializing system authorities from configuration...");
    
    var initializedCount = 0;
    var skippedCount = 0;
    
    // Initialize authorities from configuration
    for (var authorityEntry : platformConfig.authorities().definitions().entrySet()) {
      var authorityName = authorityEntry.getKey();
      var authorityDefinition = authorityEntry.getValue();
      
      if (initializeAuthority(authorityName, authorityDefinition)) {
        initializedCount++;
      } else {
        skippedCount++;
      }
    }
    
    var totalConfigured = platformConfig.authorities().definitions().size();
    
    logger.info("Authority initialization complete. Created: {}, Skipped: {}, Total configured: {}", 
        initializedCount, skippedCount, totalConfigured);
    
    // Log important authorities for visibility
    logImportantAuthorities();
  }

  /**
   * Initialize a specific authority from configuration if it doesn't exist.
   * 
   * @param authorityName the authority name to initialize
   * @param authorityDefinition the authority definition from configuration
   * @return true if the authority was created, false if it already existed
   */
  private boolean initializeAuthority(String authorityName, PlatformConfigurationProperties.AuthorityDefinition authorityDefinition) {
    var existingAuthority = authorityRepository.findByName(authorityName);
    
    if (existingAuthority.isPresent()) {
      logger.debug("Authority already exists: {} - {}", 
          authorityName, authorityDefinition.displayName());
      return false;
    }
    
    var newAuthority = new Authority(authorityName, authorityDefinition.description());
    authorityRepository.save(newAuthority);
    
    logger.info("Created authority: {} - {} (Category: {}, Priority: {})", 
        authorityName, 
        authorityDefinition.displayName(),
        authorityDefinition.category(),
        authorityDefinition.priority());
    
    return true;
  }

  /**
   * Log information about important authorities for operational visibility.
   */
  private void logImportantAuthorities() {
    var adminAuthorities = platformConfig.authorities().adminAuthorities();
    var defaultAuthorities = platformConfig.authorities().defaultAuthorities();
    
    logger.info("Admin authorities: {}", adminAuthorities);
    logger.info("Default authorities for new users: {}", defaultAuthorities);
    
    // Log CRM authorities if they exist in configuration
    var crmAuthorities = platformConfig.authorities().definitions().entrySet().stream()
        .filter(entry -> "crm".equals(entry.getValue().category()))
        .map(java.util.Map.Entry::getKey)
        .toList();
    
    if (!crmAuthorities.isEmpty()) {
      logger.info("CRM authorities available: {}", crmAuthorities);
    }
    
    // Log billing authorities if they exist in configuration
    var billingAuthorities = platformConfig.authorities().definitions().entrySet().stream()
        .filter(entry -> "billing".equals(entry.getValue().category()))
        .map(java.util.Map.Entry::getKey)
        .toList();
    
    if (!billingAuthorities.isEmpty()) {
      logger.info("Billing authorities available: {}", billingAuthorities);
    }
  }

  /**
   * Manually initialize authorities (useful for testing or manual operations).
   * 
   * @return the number of authorities created
   */
  public int manualInitialization() {
    logger.info("Manual authority initialization requested");
    
    var createdCount = 0;
    
    // Initialize from configuration
    for (var authorityEntry : platformConfig.authorities().definitions().entrySet()) {
      if (initializeAuthority(authorityEntry.getKey(), authorityEntry.getValue())) {
        createdCount++;
      }
    }
    
    logger.info("Manual initialization complete. Created {} authorities", createdCount);
    return createdCount;
  }

  /**
   * Check if all configured authorities exist in the database.
   * 
   * @return true if all authorities exist, false otherwise
   */
  public boolean areAllAuthoritiesInitialized() {
    // Check configured authorities
    for (var authorityName : platformConfig.authorities().getAuthorityNames()) {
      if (authorityRepository.findByName(authorityName).isEmpty()) {
        return false;
      }
    }
    
    return true;
  }

  /**
   * Get the count of missing authorities.
   * 
   * @return number of authorities that need to be created
   */
  public long getMissingAuthorityCount() {
    var missingCount = 0L;
    
    // Count missing configured authorities
    for (var authorityName : platformConfig.authorities().getAuthorityNames()) {
      if (authorityRepository.findByName(authorityName).isEmpty()) {
        missingCount++;
      }
    }
    
    return missingCount;
  }
}