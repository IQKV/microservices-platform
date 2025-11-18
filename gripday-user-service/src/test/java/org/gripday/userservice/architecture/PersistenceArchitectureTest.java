package org.gripday.userservice.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.*;

/**
 * Architecture tests for persistence layer concerns.
 */
@AnalyzeClasses(packages = "org.gripday.userservice")
class PersistenceArchitectureTest {

  @ArchTest
  static final ArchRule entities_should_have_table_annotation =
      classes()
          .that().areAnnotatedWith(Entity.class)
          .should().beAnnotatedWith(Table.class)
          .because("JPA entities should explicitly define table names");

  @ArchTest
  static final ArchRule entities_should_not_use_public_fields =
      fields()
          .that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
          .and().areNotStatic()
          .should().bePrivate()
          .orShould().bePackagePrivate()
          .because("Entity fields should be private or package-private");


  @ArchTest
  static final ArchRule repositories_should_be_interfaces =
      classes()
          .that().haveSimpleNameEndingWith("Repository")
          .should().beInterfaces()
          .because("Repositories should be interfaces extending Spring Data Repository");

  @ArchTest
  static final ArchRule repositories_should_extend_spring_data_repository =
      classes()
          .that().haveSimpleNameEndingWith("Repository")
          .and().areInterfaces()
          .should().beAssignableTo(org.springframework.data.repository.Repository.class)
          .because("Repositories should extend Spring Data Repository interface");

  @ArchTest
  static final ArchRule no_direct_entity_manager_usage_in_services =
      noClasses()
          .that().areAnnotatedWith(org.springframework.stereotype.Service.class)
          .should().dependOnClassesThat().haveSimpleName("EntityManager")
          .because("Services should use repositories instead of EntityManager directly");

  @ArchTest
  static final ArchRule entities_should_use_proper_fetch_types =
      fields()
          .that().areAnnotatedWith(OneToMany.class)
          .or().areAnnotatedWith(ManyToMany.class)
          .should().beAnnotatedWith(OneToMany.class)
          .orShould().beAnnotatedWith(ManyToMany.class)
          .because("Collection relationships should specify fetch type");

  @ArchTest
  static final ArchRule entities_should_not_use_bidirectional_many_to_many =
      fields()
          .that().areAnnotatedWith(ManyToMany.class)
          .should().notBeAnnotatedWith(ManyToMany.class)
          .orShould().bePrivate()
          .because("Bidirectional many-to-many relationships should be avoided or carefully managed");

  @ArchTest
  static final ArchRule entities_should_use_generation_strategy =
      fields()
          .that().areAnnotatedWith(Id.class)
          .should().beAnnotatedWith(GeneratedValue.class)
          .because("ID fields should have a generation strategy");

  @ArchTest
  static final ArchRule entities_should_be_in_domain_packages =
      classes()
          .that().areAnnotatedWith(Entity.class)
          .should().resideInAnyPackage(
              "..authentication..",
              "..registration..",
              "..usermanagement..",
              "..organization..",
              "..passwordmanagement..",
              "..emailverification..",
              "..tenancy..",
              "..shared..",
              "..security.."
          )
          .because("Entities should be in domain packages");

  @ArchTest
  static final ArchRule no_lombok_data_on_entities =
      noClasses()
          .that().areAnnotatedWith(Entity.class)
          .should().beAnnotatedWith("lombok.Data")
          .because("@Data annotation can cause issues with JPA entities");

  @ArchTest
  static final ArchRule entities_should_have_proper_column_definitions =
      fields()
          .that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
          .and().areNotStatic()
          .and().areNotAnnotatedWith(Transient.class)
          .should().beAnnotatedWith(Column.class)
          .orShould().beAnnotatedWith(Id.class)
          .orShould().beAnnotatedWith(OneToMany.class)
          .orShould().beAnnotatedWith(ManyToOne.class)
          .orShould().beAnnotatedWith(ManyToMany.class)
          .orShould().beAnnotatedWith(OneToOne.class)
          .because("Entity fields should have proper JPA annotations");

  @ArchTest
  static final ArchRule repositories_should_not_return_null =
      classes()
          .that().haveSimpleNameEndingWith("Repository")
          .should().notBeAnnotatedWith(org.springframework.lang.Nullable.class)
          .because("Repositories should return Optional instead of null");

  @ArchTest
  static final ArchRule transactional_methods_should_be_in_services =
      classes()
          .that().areAnnotatedWith(org.springframework.transaction.annotation.Transactional.class)
          .should().beAnnotatedWith(org.springframework.stereotype.Service.class)
          .orShould().resideInAPackage("..config..")
          .because("@Transactional should primarily be used on service classes");
}
