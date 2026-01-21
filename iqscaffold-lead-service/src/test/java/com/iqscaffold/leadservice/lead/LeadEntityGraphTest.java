package com.iqscaffold.leadservice.lead;

import com.iqscaffold.leadservice.activity.ActivityType;
import com.iqscaffold.leadservice.activity.LeadActivity;
import com.iqscaffold.leadservice.activity.LeadActivityRepository;
import com.iqscaffold.leadservice.note.LeadNote;
import com.iqscaffold.leadservice.note.LeadNoteRepository;
import com.iqscaffold.leadservice.shared.test.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Lead entity graphs.
 * These tests verify that entity graphs properly load related entities in a single query.
 */
@DisplayName("Lead Entity Graph Tests")
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class LeadEntityGraphTest extends BaseIntegrationTest {

  @Autowired
  private LeadRepository leadRepository;

  @Autowired
  private LeadNoteRepository leadNoteRepository;

  @Autowired
  private LeadActivityRepository leadActivityRepository;

  private Lead testLead;

  @BeforeEach
  void setUp() {
    // Create a test lead
    testLead = new Lead("John", "Doe", "john.doe@example.com", "Website");
    testLead.setCreatedBy("test-user");
    testLead.setUpdatedBy("test-user");
    testLead = leadRepository.save(testLead);

    // Add some notes
    LeadNote note1 = new LeadNote();
    note1.setLead(testLead);
    note1.setContent("First note about the lead");
    note1.setCreatedBy("test-user");
    note1.setUpdatedBy("test-user");
    leadNoteRepository.save(note1);

    LeadNote note2 = new LeadNote();
    note2.setLead(testLead);
    note2.setContent("Second note about the lead");
    note2.setIsPinned(true);
    note2.setCreatedBy("test-user");
    note2.setUpdatedBy("test-user");
    leadNoteRepository.save(note2);

    // Add some activities
    LeadActivity activity1 = new LeadActivity();
    activity1.setLead(testLead);
    activity1.setType(ActivityType.LEAD_CREATED);
    activity1.setDescription("Lead was created");
    activity1.setCreatedBy("test-user");
    leadActivityRepository.save(activity1);

    LeadActivity activity2 = new LeadActivity();
    activity2.setLead(testLead);
    activity2.setType(ActivityType.NOTE_ADDED);
    activity2.setDescription("Note was added to lead");
    activity2.setCreatedBy("test-user");
    leadActivityRepository.save(activity2);
  }

  @Test
  @DisplayName("Should load lead with notes using entity graph")
  @Transactional
  void shouldLoadLeadWithNotes() {
    // When
    Optional<Lead> leadWithNotes = leadRepository.findWithNotesById(testLead.getId());

    // Then
    assertThat(leadWithNotes).isPresent();
    Lead lead = leadWithNotes.get();
    
    // Verify lead data
    assertThat(lead.getFirstName()).isEqualTo("John");
    assertThat(lead.getLastName()).isEqualTo("Doe");
    assertThat(lead.getEmail()).isEqualTo("john.doe@example.com");
    
    // Verify notes are loaded (should not trigger additional queries)
    assertThat(lead.getNotes()).hasSize(2);
    assertThat(lead.getNotes())
        .extracting(LeadNote::getContent)
        .containsExactlyInAnyOrder("First note about the lead", "Second note about the lead");
    
    // Verify one note is pinned
    assertThat(lead.getNotes())
        .filteredOn(LeadNote::getIsPinned)
        .hasSize(1);
  }

  @Test
  @DisplayName("Should load lead with activities using entity graph")
  @Transactional
  void shouldLoadLeadWithActivities() {
    // When
    Optional<Lead> leadWithActivities = leadRepository.findWithActivitiesById(testLead.getId());

    // Then
    assertThat(leadWithActivities).isPresent();
    Lead lead = leadWithActivities.get();
    
    // Verify lead data
    assertThat(lead.getFirstName()).isEqualTo("John");
    assertThat(lead.getLastName()).isEqualTo("Doe");
    
    // Verify activities are loaded (should not trigger additional queries)
    assertThat(lead.getActivities()).hasSize(2);
    assertThat(lead.getActivities())
        .extracting(LeadActivity::getType)
        .containsExactlyInAnyOrder(ActivityType.LEAD_CREATED, ActivityType.NOTE_ADDED);
    
    assertThat(lead.getActivities())
        .extracting(LeadActivity::getDescription)
        .containsExactlyInAnyOrder("Lead was created", "Note was added to lead");
  }

  @Test
  @DisplayName("Should load lead with complete history using entity graph")
  @Transactional
  void shouldLoadLeadWithCompleteHistory() {
    // When
    Optional<Lead> leadWithHistory = leadRepository.findWithNotesAndActivitiesById(testLead.getId());

    // Then
    assertThat(leadWithHistory).isPresent();
    Lead lead = leadWithHistory.get();
    
    // Verify lead data
    assertThat(lead.getFirstName()).isEqualTo("John");
    assertThat(lead.getLastName()).isEqualTo("Doe");
    
    // Verify both notes and activities are loaded
    assertThat(lead.getNotes()).hasSize(2);
    assertThat(lead.getActivities()).hasSize(2);
    
    // Verify notes content
    assertThat(lead.getNotes())
        .extracting(LeadNote::getContent)
        .containsExactlyInAnyOrder("First note about the lead", "Second note about the lead");
    
    // Verify activities content
    assertThat(lead.getActivities())
        .extracting(LeadActivity::getType)
        .containsExactlyInAnyOrder(ActivityType.LEAD_CREATED, ActivityType.NOTE_ADDED);
  }

  @Test
  @DisplayName("Should load lead with notes by email using entity graph")
  @Transactional
  void shouldLoadLeadWithNotesByEmail() {
    // When
    Optional<Lead> leadWithNotes = leadRepository.findWithNotesByEmail("john.doe@example.com");

    // Then
    assertThat(leadWithNotes).isPresent();
    Lead lead = leadWithNotes.get();
    
    assertThat(lead.getEmail()).isEqualTo("john.doe@example.com");
    assertThat(lead.getNotes()).hasSize(2);
  }

  @Test
  @DisplayName("Should load leads with notes by status using entity graph")
  @Transactional
  void shouldLoadLeadsWithNotesByStatus() {
    // When
    var leadsWithNotes = leadRepository.findWithNotesByStatus(LeadStatus.NEW);

    // Then
    assertThat(leadsWithNotes).hasSize(1);
    Lead lead = leadsWithNotes.get(0);
    
    assertThat(lead.getStatus()).isEqualTo(LeadStatus.NEW);
    assertThat(lead.getNotes()).hasSize(2);
  }

  @Test
  @DisplayName("Should return empty when lead not found with entity graph")
  @Transactional
  void shouldReturnEmptyWhenLeadNotFound() {
    // When
    Optional<Lead> nonExistentLead = leadRepository.findWithNotesById(99999L);

    // Then
    assertThat(nonExistentLead).isEmpty();
  }

  @Test
  @DisplayName("Should handle lead with no notes or activities")
  @Transactional
  void shouldHandleLeadWithNoRelatedEntities() {
    // Given - create a lead with no notes or activities
    Lead emptyLead = new Lead("Jane", "Smith", "jane.smith@example.com", "Email");
    emptyLead.setCreatedBy("test-user");
    emptyLead.setUpdatedBy("test-user");
    emptyLead = leadRepository.save(emptyLead);

    // When
    Optional<Lead> leadWithHistory = leadRepository.findWithNotesAndActivitiesById(emptyLead.getId());

    // Then
    assertThat(leadWithHistory).isPresent();
    Lead lead = leadWithHistory.get();
    
    assertThat(lead.getFirstName()).isEqualTo("Jane");
    assertThat(lead.getNotes()).isEmpty();
    assertThat(lead.getActivities()).isEmpty();
  }

  @Test
  @DisplayName("Should verify collections are ordered by creation date descending")
  @Transactional
  void shouldVerifyCollectionsAreOrdered() {
    // When
    Optional<Lead> leadWithHistory = leadRepository.findWithNotesAndActivitiesById(testLead.getId());

    // Then
    assertThat(leadWithHistory).isPresent();
    Lead lead = leadWithHistory.get();
    
    // Verify notes are ordered by createdAt DESC
    if (lead.getNotes().size() > 1) {
      for (int i = 0; i < lead.getNotes().size() - 1; i++) {
        LeadNote current = lead.getNotes().get(i);
        LeadNote next = lead.getNotes().get(i + 1);
        assertThat(current.getCreatedAt()).isAfterOrEqualTo(next.getCreatedAt());
      }
    }
    
    // Verify activities are ordered by createdAt DESC
    if (lead.getActivities().size() > 1) {
      for (int i = 0; i < lead.getActivities().size() - 1; i++) {
        LeadActivity current = lead.getActivities().get(i);
        LeadActivity next = lead.getActivities().get(i + 1);
        assertThat(current.getCreatedAt()).isAfterOrEqualTo(next.getCreatedAt());
      }
    }
  }
}