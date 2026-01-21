package com.iqscaffold.contactservice.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.List;

import com.iqscaffold.contactservice.company.Company;
import com.iqscaffold.contactservice.company.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test class demonstrating entity graph functionality for contact entities.
 * 
 * <p>These tests verify that entity graphs properly load associations
 * without causing lazy loading exceptions or N+1 query problems.
 */
@DataJpaTest
@ActiveProfiles("test")
class ContactEntityGraphTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private ContactRepository contactRepository;

  @Autowired
  private CompanyRepository companyRepository;

  @Test
  void shouldLoadContactWithCompanyUsingEntityGraph() {
    // Given: A contact with company
    var company = new Company("Tech Corp");
    company.setIndustry("Technology");
    company.setWebsite("https://techcorp.com");
    company.setCreatedBy("test");
    company.setUpdatedBy("test");
    entityManager.persist(company);

    var contact = new Contact("John", "Doe", "john.doe@techcorp.com");
    contact.setCompanyId(company.getId());
    contact.setJobTitle("Software Engineer");
    contact.setStatus(ContactStatus.ACTIVE);
    contact.setLeadScore(75);
    contact.setCreatedBy("test");
    contact.setUpdatedBy("test");
    entityManager.persistAndFlush(contact);
    entityManager.clear(); // Clear persistence context

    // When: Finding contact with company entity graph
    var foundContact = contactRepository.findByIdWithCompany(contact.getId());

    // Then: Contact and company should be loaded
    assertThat(foundContact).isPresent();
    
    // Verify company is loaded without lazy loading exception
    assertDoesNotThrow(() -> {
      var loadedContact = foundContact.get();
      var loadedCompany = loadedContact.getCompany();
      
      assertThat(loadedCompany).isNotNull();
      assertThat(loadedCompany.getName()).isEqualTo("Tech Corp");
      assertThat(loadedCompany.getIndustry()).isEqualTo("Technology");
      assertThat(loadedCompany.getWebsite()).isEqualTo("https://techcorp.com");
    });
  }

  @Test
  void shouldLoadContactsByCompanyWithCompanyDetailsUsingEntityGraph() {
    // Given: Multiple contacts for the same company
    var company = new Company("Innovation Labs");
    company.setIndustry("Research");
    company.setCreatedBy("test");
    company.setUpdatedBy("test");
    entityManager.persist(company);

    var contact1 = new Contact("Alice", "Smith", "alice@innovation.com");
    contact1.setCompanyId(company.getId());
    contact1.setJobTitle("Research Director");
    contact1.setStatus(ContactStatus.ACTIVE);
    contact1.setCreatedBy("test");
    contact1.setUpdatedBy("test");
    entityManager.persist(contact1);

    var contact2 = new Contact("Bob", "Johnson", "bob@innovation.com");
    contact2.setCompanyId(company.getId());
    contact2.setJobTitle("Senior Researcher");
    contact2.setStatus(ContactStatus.ACTIVE);
    contact2.setCreatedBy("test");
    contact2.setUpdatedBy("test");
    entityManager.persist(contact2);

    entityManager.flush();
    entityManager.clear(); // Clear persistence context

    // When: Finding contacts by company with company entity graph
    var foundContacts = contactRepository.findByCompanyIdWithCompany(company.getId());

    // Then: All contacts and their company should be loaded
    assertThat(foundContacts).hasSize(2);
    
    // Verify company details are loaded for all contacts without lazy loading exceptions
    assertDoesNotThrow(() -> {
      for (final var contact : foundContacts) {
        var loadedCompany = contact.getCompany();
        assertThat(loadedCompany).isNotNull();
        assertThat(loadedCompany.getName()).isEqualTo("Innovation Labs");
        assertThat(loadedCompany.getIndustry()).isEqualTo("Research");
      }
    });
  }

  @Test
  void shouldLoadContactsBatchWithoutN1QueryProblem() {
    // Given: Multiple contacts with different companies
    var company1 = new Company("Company One");
    company1.setCreatedBy("test");
    company1.setUpdatedBy("test");
    entityManager.persist(company1);

    var company2 = new Company("Company Two");
    company2.setCreatedBy("test");
    company2.setUpdatedBy("test");
    entityManager.persist(company2);

    var contact1 = new Contact("Contact", "One", "contact1@company1.com");
    contact1.setCompanyId(company1.getId());
    contact1.setCreatedBy("test");
    contact1.setUpdatedBy("test");
    entityManager.persist(contact1);

    var contact2 = new Contact("Contact", "Two", "contact2@company2.com");
    contact2.setCompanyId(company2.getId());
    contact2.setCreatedBy("test");
    contact2.setUpdatedBy("test");
    entityManager.persist(contact2);

    var contact3 = new Contact("Contact", "Three", "contact3@company1.com");
    contact3.setCompanyId(company1.getId());
    contact3.setCreatedBy("test");
    contact3.setUpdatedBy("test");
    entityManager.persist(contact3);

    entityManager.flush();
    entityManager.clear(); // Clear persistence context

    var contactIds = List.of(contact1.getId(), contact2.getId(), contact3.getId());

    // When: Finding multiple contacts by IDs in batch
    var foundContacts = contactRepository.findAllByIdIn(contactIds);

    // Then: All contacts should be loaded in a single query
    assertThat(foundContacts).hasSize(3);
    
    // Verify all contacts are loaded
    var contactEmails = foundContacts.stream()
        .map(Contact::getEmail)
        .toList();
    
    assertThat(contactEmails).containsExactlyInAnyOrder(
        "contact1@company1.com",
        "contact2@company2.com", 
        "contact3@company1.com"
    );
  }

  @Test
  void shouldLoadContactsBatchWithCompanyDetailsUsingEntityGraph() {
    // Given: Multiple contacts with companies
    var company1 = new Company("Batch Company One");
    company1.setIndustry("Finance");
    company1.setCreatedBy("test");
    company1.setUpdatedBy("test");
    entityManager.persist(company1);

    var company2 = new Company("Batch Company Two");
    company2.setIndustry("Healthcare");
    company2.setCreatedBy("test");
    company2.setUpdatedBy("test");
    entityManager.persist(company2);

    var contact1 = new Contact("Batch", "Contact1", "batch1@finance.com");
    contact1.setCompanyId(company1.getId());
    contact1.setLeadScore(90);
    contact1.setCreatedBy("test");
    contact1.setUpdatedBy("test");
    entityManager.persist(contact1);

    var contact2 = new Contact("Batch", "Contact2", "batch2@healthcare.com");
    contact2.setCompanyId(company2.getId());
    contact2.setLeadScore(85);
    contact2.setCreatedBy("test");
    contact2.setUpdatedBy("test");
    entityManager.persist(contact2);

    entityManager.flush();
    entityManager.clear(); // Clear persistence context

    var contactIds = List.of(contact1.getId(), contact2.getId());

    // When: Finding multiple contacts by IDs with company details
    var foundContacts = contactRepository.findAllByIdInWithCompany(contactIds);

    // Then: All contacts and their companies should be loaded
    assertThat(foundContacts).hasSize(2);
    
    // Verify company details are loaded without lazy loading exceptions
    assertDoesNotThrow(() -> {
      var contactsByEmail = foundContacts.stream()
          .collect(java.util.stream.Collectors.toMap(Contact::getEmail, c -> c));
      
      var financeContact = contactsByEmail.get("batch1@finance.com");
      assertThat(financeContact).isNotNull();
      assertThat(financeContact.getCompany()).isNotNull();
      assertThat(financeContact.getCompany().getName()).isEqualTo("Batch Company One");
      assertThat(financeContact.getCompany().getIndustry()).isEqualTo("Finance");
      
      var healthcareContact = contactsByEmail.get("batch2@healthcare.com");
      assertThat(healthcareContact).isNotNull();
      assertThat(healthcareContact.getCompany()).isNotNull();
      assertThat(healthcareContact.getCompany().getName()).isEqualTo("Batch Company Two");
      assertThat(healthcareContact.getCompany().getIndustry()).isEqualTo("Healthcare");
    });
  }

  @Test
  void shouldLoadHighValueContactsWithCompanyUsingEntityGraph() {
    // Given: Contacts with different lead scores
    var company = new Company("High Value Corp");
    company.setIndustry("Enterprise Software");
    company.setCreatedBy("test");
    company.setUpdatedBy("test");
    entityManager.persist(company);

    var highValueContact = new Contact("High", "Value", "high@corp.com");
    highValueContact.setCompanyId(company.getId());
    highValueContact.setLeadScore(95);
    highValueContact.setJobTitle("CTO");
    highValueContact.setCreatedBy("test");
    highValueContact.setUpdatedBy("test");
    entityManager.persist(highValueContact);

    var lowValueContact = new Contact("Low", "Value", "low@corp.com");
    lowValueContact.setCompanyId(company.getId());
    lowValueContact.setLeadScore(30);
    lowValueContact.setJobTitle("Intern");
    lowValueContact.setCreatedBy("test");
    lowValueContact.setUpdatedBy("test");
    entityManager.persist(lowValueContact);

    entityManager.flush();
    entityManager.clear(); // Clear persistence context

    // When: Finding high-value contacts (score >= 80) with company details
    var highValueContacts = contactRepository.findByLeadScoreGreaterThanEqualWithCompany(80);

    // Then: Only high-value contact should be returned with company details
    assertThat(highValueContacts).hasSize(1);
    
    // Verify company details are loaded without lazy loading exceptions
    assertDoesNotThrow(() -> {
      var contact = highValueContacts.get(0);
      assertThat(contact.getEmail()).isEqualTo("high@corp.com");
      assertThat(contact.getLeadScore()).isEqualTo(95);
      
      var company1 = contact.getCompany();
      assertThat(company1).isNotNull();
      assertThat(company1.getName()).isEqualTo("High Value Corp");
      assertThat(company1.getIndustry()).isEqualTo("Enterprise Software");
    });
  }
}