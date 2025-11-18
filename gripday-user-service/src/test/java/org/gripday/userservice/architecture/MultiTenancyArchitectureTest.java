package org.gripday.userservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;

/**
 * Architecture tests for multi-tenancy concerns.
 */
@AnalyzeClasses(packages = "org.gripday.userservice")
class MultiTenancyArchitectureTest {

  @ArchTest
  static final ArchRule tenant_aware_entities_should_extend_base_class =
      classes()
          .that().areAnnotatedWith(Entity.class)
          .and().resideInAnyPackage(
              "..usermanagement..",
              "..organization..",
              "..authentication..",
              "..emailverification.."
          )
          .should().beAssignableTo("org.gripday.userservice.shared.TenantAware")
          .orShould().haveSimpleNameContaining("Authority")
          .orShould().haveSimpleNameContaining("Audit")
          .because("Domain entities should extend TenantAware for multi-tenancy support");

  @ArchTest
  static final ArchRule tenant_context_should_be_used_in_services =
      classes()
          .that().areAnnotatedWith(org.springframework.stereotype.Service.class)
          .and().resideInAnyPackage(
              "..usermanagement..",
              "..organization..",
              "..tenancy.."
          )
          .should().dependOnClassesThat().haveSimpleName("TenantContext")
          .orShould().haveSimpleNameContaining("Email")
          .orShould().haveSimpleNameContaining("Jwt")
          .because("Services handling tenant data should use TenantContext");

  @ArchTest
  static final ArchRule tenant_extraction_should_be_in_filter =
      classes()
          .that().haveSimpleNameContaining("TenantExtraction")
          .should().beAssignableTo(jakarta.servlet.Filter.class)
          .orShould().beAnnotatedWith(org.springframework.stereotype.Service.class)
          .because("Tenant extraction should be done in filters or services");

  @ArchTest
  static final ArchRule tenant_config_should_be_in_tenancy_package =
      classes()
          .that().haveSimpleNameContaining("Tenant")
          .and().haveSimpleNameEndingWith("Config")
          .should().resideInAPackage("..tenancy..")
          .because("Tenant configuration should be in tenancy package");

  @ArchTest
  static final ArchRule tenant_repository_should_be_in_tenancy_package =
      classes()
          .that().haveSimpleName("TenantRepository")
          .should().resideInAPackage("..tenancy..")
          .because("Tenant repository should be in tenancy package");

  @ArchTest
  static final ArchRule tenant_aware_base_class_should_be_in_shared =
      classes()
          .that().haveSimpleName("TenantAware")
          .should().resideInAPackage("..shared..")
          .because("TenantAware base class should be in shared package");

  @ArchTest
  static final ArchRule tenant_services_should_validate_tenant_access =
      methods()
          .that().areDeclaredInClassesThat().resideInAPackage("..tenancy..")
          .and().arePublic()
          .and().haveNameMatching(".*(create|update|delete).*")
          .should().beAnnotatedWith(org.springframework.transaction.annotation.Transactional.class)
          .because("Tenant modification methods should be transactional");

  @ArchTest
  static final ArchRule no_cross_tenant_data_access =
      classes()
          .that().areAnnotatedWith(org.springframework.stereotype.Service.class)
          .should().notDependOnClassesThat().haveSimpleNameContaining("CrossTenant")
          .because("Services should not allow cross-tenant data access");

  @ArchTest
  static final ArchRule tenant_isolation_should_be_enforced =
      classes()
          .that().haveSimpleNameEndingWith("Repository")
          .and().resideInAnyPackage(
              "..usermanagement..",
              "..organization..",
              "..authentication.."
          )
          .should().beInterfaces()
          .because("Repositories should be interfaces to allow tenant isolation");
}
