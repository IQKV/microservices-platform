package com.iqscaffold.contactservice.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.iqscaffold.contactservice.contact.Contact;
import com.iqscaffold.contactservice.contact.ContactRepository;
import com.iqscaffold.contactservice.contact.ContactStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test class demonstrating entity graph functionality for company entities.
 * 
 * <p>
 * These tests verify that entity graphs properly load associations
 * without causing lazy loading exceptions or N+1 query problems.
 */
@DataJpaTest
@ActiveProfiles("test")
class CompanyEntityGraphTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private CompanyRepository companyRepository;

  @Autowired
  private ContactRepository contactRepository;

  @Test
  void shouldLoadCompanyWithContactsUsingEntityGraph() {
    // Given: A company with multiple contacts
    var company = new Company("Contact Heavy Corp");
    company.setIndustry("Consulting");
    company.setWebsite("https://contactheavy.com");
    company.setCreatedBy("test");
    company.setUpdatedBy("test");
    entityManager.persist(company);

    var contact1 = new Contact("Contact", "One", "contact1@contactheavy.com");
    contact1.setCompanyId(company.getId());
    contact1.setJobTitle("Consultant");
    contact1.setStatus(ContactStatus.ACTIVE);
    contact1.setCreatedBy("test");
    contact1.setUpdatedBy("test");
    entityManager.persist(contact1);

    var contact2 = new Contact("Contact", "Two", "contact2@contactheavy.com");
    contact2.setCompanyId(company.getId());
    contact2.setJobTitle("Senior Consultant");
    contact2.setStatus(ContactStatus.ACTIVE);
    contact2.setCreatedBy("test");
    contact2.setUpdatedBy("test");
    entityManager.persist(contact2);

    entityManager.flush();
    entityManager.clear(); // Clear persistence context

    // When: Finding company with contacts entity graph
    var foundCompany = companyRepository.findByIdWithContacts(company.getId());

    // Then: Company and contacts should be loaded
    assertThat(foundCompany).isPresent();

    // Verify contacts are loaded without lazy loading exception
    assertDoesNotThrow(() -> {
      var loadedCompany = foundCompany.get();
      var contacts = loadedCompany.getContacts();

      assertThat(contacts).hasSize(2);

      var contactEmails = contacts.stream()
          .map(Contact::getEmail)
          .toList();

      assertThat(contactEmails).containsExactlyInAnyOrder(
          "contact1@contactheavy.com",
          "contact2@contactheavy.com");
    });
  }

  @Test
  void shouldLoadCompanyHierarchyUsingEntityGraph() {
    // Given: A company hierarchy (parent -> child -> grandchild)
    var parentCompany = new Company("Parent Corp");
    parentCompany.setIndustry("Holding Company");
    parentCompany.setCreatedBy("test");
    parentCompany.setUpdatedBy("test");
    entityManager.persist(parentCompany);

    var childCompany = new Company("Child Corp");
    childCompany.setParentCompanyId(parentCompany.getId());
    childCompany.setIndustry("Technology");
    childCompany.setCreatedBy("test");
    childCompany.setUpdatedBy("test");
    entityManager.persist(childCompany);

    var grandchildCompany = new Company("Grandchild Corp");
    grandchildCompany.setParentCompanyId(childCompany.getId());
    grandchildCompany.setIndustry("Software");
    grandchildCompany.setCreatedBy("test");
    grandchildCompany.setUpdatedBy("test");
    entityManager.persist(grandchildCompany);

    entityManager.flush();
    entityManager.clear(); // Clear persistence context

    // When: Finding child company with hierarchy entity graph
    var foundCompany = companyRepository.findByIdWithHierarchy(childCompany.getId());

    // Then: Company with parent and children should be loaded
    assertThat(foundCompany).isPresent();

    // Verify hierarchy is loaded without lazy loading exceptions
    assertDoesNotThrow(() -> {
      var loadedCompany = foundCompany.get();

      // Check parent company
      var parent = loadedCompany.getParentCompany();
      assertThat(parent).isNotNull();
      assertThat(parent.getName()).isEqualTo("Parent Corp");
      assertThat(parent.getIndustry()).isEqualTo("Holding Company");

      // Check child companies
      var children = loadedCompany.getChildCompanies();
      assertThat(children).hasSize(1);
      var firstChild = children.iterator().next();
      assertThat(firstChild.getName()).isEqualTo("Grandchild Corp");
      assertThat(firstChild.getIndustry()).isEqualTo("Software");
    });
  }

  @Test
  void shouldLoadCompanyWithParentUsingEntityGraph() {
    // Given: A subsidiary company with parent
    var parentCompany = new Company("Global Holdings");
    parentCompany.setIndustry("Investment");
    parentCompany.setSize("Large");
    parentCompany.setCreatedBy("test");
    parentCompany.setUpdatedBy("test");
    entityManager.persist(parentCompany);

    var subsidiaryCompany = new Company("Regional Operations");
    subsidiaryCompany.setParentCompanyId(parentCompany.getId());
    subsidiaryCompany.setIndustry("Operations");
    subsidiaryCompany.setSize("Medium");
    subsidiaryCompany.setCreatedBy("test");
    subsidiaryCompany.setUpdatedBy("test");
    entityManager.persist(subsidiaryCompany);

    entityManager.flush();
    entityManager.clear(); // Clear persistence context

    // When: Finding subsidiary with parent entity graph
    var foundCompany = companyRepository.findByIdWithParent(subsidiaryCompany.getId());

    // Then: Company and parent should be loaded
    assertThat(foundCompany).isPresent();

    // Verify parent is loaded without lazy loading exception
    assertDoesNotThrow(() -> {
      var loadedCompany = foundCompany.get();
      assertThat(loadedCompany.getName()).isEqualTo("Regional Operations");

      var parent = loadedCompany.getParentCompany();
      assertThat(parent).isNotNull();
      assertThat(parent.getName()).isEqualTo("Global Holdings");
      assertThat(parent.getIndustry()).isEqualTo("Investment");
      assertThat(parent.getSize()).isEqualTo("Large");
    });
  }

  @Test
  void shouldLoadCompanyWithChildrenUsingEntityGraph() {
    // Given: A parent company with multiple subsidiaries
    var parentCompany = new Company("Conglomerate Inc");
    parentCompany.setIndustry("Diversified");
    parentCompany.setCreatedBy("test");
    parentCompany.setUpdatedBy("test");
    entityManager.persist(parentCompany);

    var subsidiary1 = new Company("Tech Division");
    subsidiary1.setParentCompanyId(parentCompany.getId());
    subsidiary1.setIndustry("Technology");
    subsidiary1.setCreatedBy("test");
    subsidiary1.setUpdatedBy("test");
    entityManager.persist(subsidiary1);

    var subsidiary2 = new Company("Finance Division");
    subsidiary2.setParentCompanyId(parentCompany.getId());
    subsidiary2.setIndustry("Financial Services");
    subsidiary2.setCreatedBy("test");
    subsidiary2.setUpdatedBy("test");
    entityManager.persist(subsidiary2);

    entityManager.flush();
    entityManager.clear(); // Clear persistence context

    // When: Finding parent with children entity graph
    var foundCompany = companyRepository.findByIdWithChildren(parentCompany.getId());

    // Then: Company and children should be loaded
    assertThat(foundCompany).isPresent();

    // Verify children are loaded without lazy loading exceptions
    assertDoesNotThrow(() -> {
      var loadedCompany = foundCompany.get();
      assertThat(loadedCompany.getName()).isEqualTo("Conglomerate Inc");

      var children = loadedCompany.getChildCompanies();
      assertThat(children).hasSize(2);

      var childNames = children.stream()
          .map(Company::getName)
          .toList();

      assertThat(childNames).containsExactlyInAnyOrder(
          "Tech Division",
          "Finance Division");

      var childIndustries = children.stream()
          .map(Company::getIndustry)
          .toList();

      assertThat(childIndustries).containsExactlyInAnyOrder(
          "Technology",
          "Financial Services");
    });
  }

  @Test
  void shouldLoadCompanyCompleteWithAllRelationshipsUsingEntityGraph() {
    // Given: A complex company structure with all relationships
    var grandparentCompany = new Company("Grandparent Corp");
    grandparentCompany.setIndustry("Holding");
    grandparentCompany.setCreatedBy("test");
    grandparentCompany.setUpdatedBy("test");
    entityManager.persist(grandparentCompany);

    var parentCompany = new Company("Parent Corp");
    parentCompany.setParentCompanyId(grandparentCompany.getId());
    parentCompany.setIndustry("Management");
    parentCompany.setCreatedBy("test");
    parentCompany.setUpdatedBy("test");
    entityManager.persist(parentCompany);

    var childCompany = new Company("Child Corp");
    childCompany.setParentCompanyId(parentCompany.getId());
    childCompany.setIndustry("Operations");
    childCompany.setCreatedBy("test");
    childCompany.setUpdatedBy("test");
    entityManager.persist(childCompany);

    // Add contacts to parent company
    var contact1 = new Contact("Manager", "One", "manager1@parent.com");
    contact1.setCompanyId(parentCompany.getId());
    contact1.setJobTitle("General Manager");
    contact1.setCreatedBy("test");
    contact1.setUpdatedBy("test");
    entityManager.persist(contact1);

    var contact2 = new Contact("Manager", "Two", "manager2@parent.com");
    contact2.setCompanyId(parentCompany.getId());
    contact2.setJobTitle("Operations Manager");
    contact2.setCreatedBy("test");
    contact2.setUpdatedBy("test");
    entityManager.persist(contact2);

    entityManager.flush();
    entityManager.clear(); // Clear persistence context

    // When: Finding parent company with complete entity graph
    var foundCompany = companyRepository.findByIdComplete(parentCompany.getId());

    // Then: Company with all relationships should be loaded
    assertThat(foundCompany).isPresent();

    // Verify all relationships are loaded without lazy loading exceptions
    assertDoesNotThrow(() -> {
      var loadedCompany = foundCompany.get();
      assertThat(loadedCompany.getName()).isEqualTo("Parent Corp");

      // Check parent company
      var parent = loadedCompany.getParentCompany();
      assertThat(parent).isNotNull();
      assertThat(parent.getName()).isEqualTo("Grandparent Corp");

      // Check child companies
      var children = loadedCompany.getChildCompanies();
      assertThat(children).hasSize(1);
      assertThat(children.iterator().next().getName()).isEqualTo("Child Corp");

      // Check contacts
      var contacts = loadedCompany.getContacts();
      assertThat(contacts).hasSize(2);

      var contactEmails = contacts.stream()
          .map(Contact::getEmail)
          .toList();

      assertThat(contactEmails).containsExactlyInAnyOrder(
          "manager1@parent.com",
          "manager2@parent.com");
    });
  }

  @Test
  void shouldLoadParentCompaniesWithChildrenUsingEntityGraph() {
    // Given: Multiple parent companies with children
    var parent1 = new Company("Parent One");
    parent1.setIndustry("Tech");
    parent1.setCreatedBy("test");
    parent1.setUpdatedBy("test");
    entityManager.persist(parent1);

    var parent2 = new Company("Parent Two");
    parent2.setIndustry("Finance");
    parent2.setCreatedBy("test");
    parent2.setUpdatedBy("test");
    entityManager.persist(parent2);

    var child1 = new Company("Child One");
    child1.setParentCompanyId(parent1.getId());
    child1.setIndustry("Software");
    child1.setCreatedBy("test");
    child1.setUpdatedBy("test");
    entityManager.persist(child1);

    var child2 = new Company("Child Two");
    child2.setParentCompanyId(parent2.getId());
    child2.setIndustry("Banking");
    child2.setCreatedBy("test");
    child2.setUpdatedBy("test");
    entityManager.persist(child2);

    entityManager.flush();
    entityManager.clear(); // Clear persistence context

    // When: Finding all parent companies with children
    var parentCompanies = companyRepository.findParentCompaniesWithChildren();

    // Then: All parent companies with their children should be loaded
    assertThat(parentCompanies).hasSize(2);

    // Verify children are loaded without lazy loading exceptions
    assertDoesNotThrow(() -> {
      var parentsByName = parentCompanies.stream()
          .collect(java.util.stream.Collectors.toMap(Company::getName, c -> c));

      var parentOne = parentsByName.get("Parent One");
      assertThat(parentOne).isNotNull();
      assertThat(parentOne.getChildCompanies()).hasSize(1);
      assertThat(parentOne.getChildCompanies().iterator().next().getName()).isEqualTo("Child One");

      var parentTwo = parentsByName.get("Parent Two");
      assertThat(parentTwo).isNotNull();
      assertThat(parentTwo.getChildCompanies()).hasSize(1);
      assertThat(parentTwo.getChildCompanies().iterator().next().getName()).isEqualTo("Child Two");
    });
  }
}