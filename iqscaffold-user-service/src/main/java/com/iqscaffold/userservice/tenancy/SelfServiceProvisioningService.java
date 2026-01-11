package com.iqscaffold.userservice.tenancy;

import com.iqscaffold.userservice.emailverification.EmailVerificationService;
import com.iqscaffold.userservice.infrastructure.repository.dto.TenantDto.CreateTenantRequest;
import com.iqscaffold.userservice.organization.OrganizationManagementService;
import com.iqscaffold.userservice.security.InputSanitizer;
import com.iqscaffold.userservice.security.SecurityAuditService;
import com.iqscaffold.userservice.shared.Authority;
import com.iqscaffold.userservice.shared.AuthorityRepository;
import com.iqscaffold.userservice.usermanagement.User;
import com.iqscaffold.userservice.usermanagement.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for self-service tenant provisioning.
 * Orchestrates the complete workflow of creating a tenant, organization, and admin user.
 */
@Service
public class SelfServiceProvisioningService {

  private static final Logger logger = LoggerFactory.getLogger(SelfServiceProvisioningService.class);

  // Default quotas for new tenants
  private static final int DEFAULT_MAX_USERS = 10;
  private static final int DEFAULT_STORAGE_QUOTA_GB = 1;
  private static final int DEFAULT_API_RATE_LIMIT = 1000;

  private final TenantManagementService tenantManagementService;
  private final OrganizationManagementService organizationManagementService;
  private final UserRepository userRepository;
  private final AuthorityRepository authorityRepository;
  private final PasswordEncoder passwordEncoder;
  private final SecurityAuditService securityAuditService;
  private final InputSanitizer inputSanitizer;
  private final EmailVerificationService emailVerificationService;
  private final TenantIdGenerator tenantIdGenerator;
  private final com.iqscaffold.userservice.infrastructure.messaging.UserEventPublisher userEventPublisher;
  private final com.iqscaffold.userservice.infrastructure.messaging.MessagingService messagingService;

  public SelfServiceProvisioningService(
      final TenantManagementService tenantManagementService,
      final OrganizationManagementService organizationManagementService,
      final UserRepository userRepository,
      final AuthorityRepository authorityRepository,
      final PasswordEncoder passwordEncoder,
      final SecurityAuditService securityAuditService,
      final InputSanitizer inputSanitizer,
      final EmailVerificationService emailVerificationService,
      final TenantIdGenerator tenantIdGenerator,
      final com.iqscaffold.userservice.infrastructure.messaging.UserEventPublisher userEventPublisher,
      final com.iqscaffold.userservice.infrastructure.messaging.MessagingService messagingService) {
    this.tenantManagementService = tenantManagementService;
    this.organizationManagementService = organizationManagementService;
    this.userRepository = userRepository;
    this.authorityRepository = authorityRepository;
    this.passwordEncoder = passwordEncoder;
    this.securityAuditService = securityAuditService;
    this.inputSanitizer = inputSanitizer;
    this.emailVerificationService = emailVerificationService;
    this.tenantIdGenerator = tenantIdGenerator;
    this.userEventPublisher = userEventPublisher;
    this.messagingService = messagingService;
  }

  /**
   * Provision a complete tenant environment with organization and admin user.
   * This method orchestrates the entire self-service signup flow:
   * 1. Validate and sanitize inputs
   * 2. Generate or validate tenant ID
   * 3. Create tenant with schema provisioning
   * 4. Create default organization
   * 5. Create admin user with TENANT_ADMIN authority
   * 6. Associate user with organization
   * 7. Send email verification
   * 8. Log audit event
   *
   * @param request signup request with tenant, org, and admin user details
   * @param ipAddress client IP address for security audit
   * @param userAgent client user agent for security audit
   * @return response with created tenant, organization, and admin user details
   * @throws SelfServiceProvisioningException if provisioning fails
   */
  @Transactional
  public SelfServiceSignupResponse provisionTenantWithAdmin(
      SelfServiceSignupRequest request,
      String ipAddress,
      String userAgent) {

    logger.info("Starting self-service provisioning for organization: {}", request.organizationName());

    try {
      // Step 1: Sanitize and validate inputs
      validateAndSanitizeInputs(request, ipAddress, userAgent);

      // Step 2: Generate or validate tenant ID
      var tenantId = generateOrValidateTenantId(request);

      // Step 3: Create tenant with schema provisioning
      var createdTenant = createTenant(request, tenantId);

      // Step 4: Publish tenant created event for downstream services
      publishTenantCreatedEvent(tenantId, request.organizationName(), createdTenant);

      // Step 5: Set tenant context for subsequent operations
      TenantContext.setCurrentTenantId(tenantId);

      try {
        // Step 6: Create default organization
        var organization = organizationManagementService.createDefaultOrganization(
            request.organizationName(),
            tenantId,
            request.adminEmail()
        );

        // Step 7: Create admin user
        var adminUser = createAdminUser(request, tenantId, organization.getId());

        // Step 8: Update organization with owner user ID
        organization.setOwnerUserId(adminUser.getId());

        // Step 9: Generate email verification token and send email
        sendVerificationEmail(adminUser);

        // Step 10: Publish user created event for downstream services
        publishUserCreatedEvent(adminUser);

        // Step 11: Log successful provisioning
        securityAuditService.logUserRegistration(
            adminUser.getUsername(),
            adminUser.getEmail(),
            ipAddress,
            userAgent
        );

        logger.info("Successfully provisioned tenant: {} with admin user: {}",
            tenantId, adminUser.getUsername());

        // Step 12: Return success response
        return SelfServiceSignupResponse.success(
            tenantId,
            request.organizationName(),
            organization.getId(),
            adminUser.getId(),
            adminUser.getUsername(),
            adminUser.getEmail(),
            adminUser.getFirstName(),
            adminUser.getLastName(),
            createdTenant.createdAt()
        );

      } finally {
        // Always clear tenant context
        TenantContext.clear();
      }

    } catch (final Exception e) {
      logger.error("Failed to provision tenant for organization: {}", request.organizationName(), e);
      
      // Log failure for security audit
      securityAuditService.logSuspiciousActivity(
          request.adminEmail(),
          "Self-service provisioning failed: " + e.getMessage(),
          ipAddress,
          userAgent
      );

      throw new SelfServiceProvisioningException(
          "Failed to provision tenant: " + e.getMessage(),
          e
      );
    }
  }

  /**
   * Validate and sanitize all inputs to prevent XSS and injection attacks.
   */
  private void validateAndSanitizeInputs(
      SelfServiceSignupRequest request,
      String ipAddress,
      String userAgent) {

    // Sanitize inputs
    var sanitizedUsername = inputSanitizer.sanitizeUsername(request.adminUsername());
    var sanitizedEmail = inputSanitizer.sanitizeEmail(request.adminEmail());

    // Check for unsafe inputs
    if (!inputSanitizer.isInputSafe(request.adminUsername())
        || !inputSanitizer.isInputSafe(request.adminEmail())
        || !inputSanitizer.isInputSafe(request.adminFirstName())
        || !inputSanitizer.isInputSafe(request.adminLastName())
        || !inputSanitizer.isInputSafe(request.organizationName())) {

      securityAuditService.logSuspiciousActivity(
          sanitizedEmail,
          "Potential XSS/injection attempt in self-service signup",
          ipAddress,
          userAgent
      );
      throw new SelfServiceProvisioningException("Invalid input detected");
    }

    // Check for SQL injection attempts
    if (inputSanitizer.containsSqlInjection(request.adminUsername())
        || inputSanitizer.containsSqlInjection(request.adminEmail())
        || inputSanitizer.containsSqlInjection(request.adminFirstName())
        || inputSanitizer.containsSqlInjection(request.adminLastName())
        || inputSanitizer.containsSqlInjection(request.organizationName())) {

      securityAuditService.logSuspiciousActivity(
          sanitizedEmail,
          "SQL injection attempt in self-service signup",
          ipAddress,
          userAgent
      );
      throw new SelfServiceProvisioningException("Invalid input detected");
    }

    // Check for duplicate username (cross-tenant)
    // Note: Username uniqueness is enforced at tenant level, but we check globally to avoid confusion
    var existingUsername = userRepository.existsByUsername(sanitizedUsername);
    if (existingUsername) {
      throw new SelfServiceProvisioningException("Username already exists");
    }

    // Check for duplicate email (cross-tenant)
    var existingEmail = userRepository.existsByEmail(sanitizedEmail);
    if (existingEmail) {
      throw new SelfServiceProvisioningException("Email already exists");
    }
  }

  /**
   * Generate a unique tenant ID or validate the provided one.
   */
  private String generateOrValidateTenantId(SelfServiceSignupRequest request) {
    var tenantId = request.tenantId();

    if (tenantId == null || tenantId.isEmpty()) {
      // Generate tenant ID from organization name
      tenantId = tenantIdGenerator.generateFromOrganizationName(request.organizationName());
      logger.info("Generated tenant ID: {} from organization: {}", tenantId, request.organizationName());
    } else {
      // Validate provided tenant ID
      if (!tenantIdGenerator.isValid(tenantId)) {
        throw new SelfServiceProvisioningException("Invalid tenant ID format");
      }
      if (!tenantIdGenerator.isAvailable(tenantId)) {
        throw new SelfServiceProvisioningException("Tenant ID already exists");
      }
      logger.info("Using provided tenant ID: {}", tenantId);
    }

    return tenantId;
  }

  /**
   * Create tenant with default quotas and schema provisioning.
   */
  private com.iqscaffold.userservice.infrastructure.repository.dto.TenantDto.TenantResponse createTenant(
      SelfServiceSignupRequest request,
      String tenantId) {

    var createTenantRequest = new CreateTenantRequest(
        tenantId,
        request.organizationName(),
        "Self-service tenant provisioning",
        request.domain(),
        DEFAULT_MAX_USERS,
        DEFAULT_STORAGE_QUOTA_GB,
        DEFAULT_API_RATE_LIMIT
    );

    return tenantManagementService.createTenant(
        createTenantRequest,
        "system:self-service"
    );
  }

  /**
   * Create admin user with TENANT_ADMIN authority within tenant context.
   */
  private User createAdminUser(
      SelfServiceSignupRequest request,
      String tenantId,
      Long organizationId) {

    // Hash password
    var hashedPassword = passwordEncoder.encode(request.adminPassword());

    // Create user entity
    var user = new User(
        request.adminUsername(),
        request.adminEmail(),
        hashedPassword,
        request.adminFirstName(),
        request.adminLastName(),
        tenantId
    );

    // Note: Organization association is implicit via tenant context
    // The organization ID is passed to this method for setting as the organization's ownerUserId
    
    // Set email as not verified (verification email will be sent)
    user.setEmailVerified(false);

    // Assign TENANT_ADMIN authority
    var tenantAdminAuthority = findOrCreateTenantAdminAuthority();
    user.addAuthority(tenantAdminAuthority);

    // Save user (within tenant context)
    return userRepository.save(user);
  }

  /**
   * Find or create TENANT_ADMIN authority.
   */
  private Authority findOrCreateTenantAdminAuthority() {
    return authorityRepository.findByName("TENANT_ADMIN")
        .orElseGet(() -> {
          logger.warn("TENANT_ADMIN authority not found, creating it");
          var authority = new Authority(
              "TENANT_ADMIN",
              "Tenant administrator with full control within tenant scope"
          );
          return authorityRepository.save(authority);
        });
  }

  /**
   * Send verification email to admin user.
   */
  private void sendVerificationEmail(User user) {
    try {
      emailVerificationService.generateVerificationToken(user);
      logger.info("Sent verification email to: {}", user.getEmail());
    } catch (final Exception e) {
      logger.warn("Failed to send verification email to user: {} ({})",
          user.getUsername(), user.getEmail(), e);
      // Don't fail provisioning if email sending fails
    }
  }

  /**
   * Publish tenant created event for downstream services (e.g., billing service).
   */
  private void publishTenantCreatedEvent(
      String tenantId,
      String organizationName,
      com.iqscaffold.userservice.infrastructure.repository.dto.TenantDto.TenantResponse tenant) {
    try {
      var metadata = new java.util.HashMap<String, Object>();
      metadata.put("maxUsers", tenant.maxUsers());
      metadata.put("storageQuotaGb", tenant.storageQuotaGb());
      metadata.put("apiRateLimitPerMinute", tenant.apiRateLimitPerMinute());
      metadata.put("domain", tenant.domain());
      
      messagingService.publishTenantCreated(tenantId, organizationName, metadata);
      logger.info("Published tenant created event for: {}", tenantId);
    } catch (final Exception e) {
      logger.warn("Failed to publish tenant created event for tenant: {}",
          tenantId, e);
      // Don't fail provisioning if event publishing fails
    }
  }

  /**
   * Publish user created event for downstream services (e.g., billing service).
   */
  private void publishUserCreatedEvent(User user) {
    try {
      userEventPublisher.publishUserCreated(user);
      logger.info("Published user created event for: {}", user.getUsername());
    } catch (final Exception e) {
      logger.warn("Failed to publish user created event for user: {}",
          user.getUsername(), e);
      // Don't fail provisioning if event publishing fails
    }
  }

  /**
   * Custom exception for self-service provisioning errors.
   */
  public static class SelfServiceProvisioningException extends RuntimeException {

    public SelfServiceProvisioningException(final String message) {
      super(message);
    }

    public SelfServiceProvisioningException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
