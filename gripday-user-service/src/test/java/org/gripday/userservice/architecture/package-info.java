/**
 * Architecture and modulith tests for the Gripday User Service.
 * <p>
 * This package contains comprehensive architectural tests that validate:
 * <ul>
 *   <li><b>Dependency Rules</b> - Ensures proper dependency direction and prevents circular dependencies</li>
 *   <li><b>Layered Architecture</b> - Validates layer separation and package structure</li>
 *   <li><b>Modularity</b> - Verifies Spring Modulith module boundaries and independence</li>
 *   <li><b>Platform Architecture</b> - Validates cross-cutting concerns and coding standards</li>
 *   <li><b>Naming Conventions</b> - Ensures consistent naming across the codebase</li>
 *   <li><b>Security Architecture</b> - Validates security-related architectural patterns</li>
 *   <li><b>Persistence Architecture</b> - Ensures proper JPA and repository patterns</li>
 *   <li><b>API Architecture</b> - Validates REST API design and conventions</li>
 *   <li><b>Multi-Tenancy</b> - Ensures proper tenant isolation and context handling</li>
 *   <li><b>Observability</b> - Validates logging, metrics, and tracing patterns</li>
 *   <li><b>Modulith Integration</b> - Tests module interactions and boundaries</li>
 * </ul>
 * <p>
 * These tests use ArchUnit and Spring Modulith to enforce architectural rules
 * and prevent architectural drift over time.
 * <p>
 * <b>Note:</b> These tests are excluded from regular test runs via maven-surefire-plugin
 * configuration and should be run explicitly during architecture reviews or CI/CD pipelines.
 *
 * @see com.tngtech.archunit.junit.AnalyzeClasses
 * @see org.springframework.modulith.core.ApplicationModules
 */

package org.gripday.userservice.architecture;
