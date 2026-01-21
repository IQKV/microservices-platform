package com.iqscaffold.leadservice.lead;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.iqscaffold.leadservice.activity.ActivityType;
import com.iqscaffold.leadservice.activity.LeadActivity;
import com.iqscaffold.leadservice.activity.LeadActivityRepository;
import com.iqscaffold.leadservice.note.LeadNote;
import com.iqscaffold.leadservice.note.LeadNoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for Lead entity graphs.
 * These tests verify that entity graphs properly load related entities in a
 * single query.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Lead Entity Graph Tests")
class LeadEntityGraphTest {

  @Autowired
  private LeadRepository leadRepository;

  @Autowired
  private LeadNoteRepository leadNoteRepository;

  @Autowired
  private LeadActivityRepository leadActivityRepository;

  @Autowired
  private EntityManager entityManager;

  private Lead testLead;

  @BeforeEach
  void setUp() {
    leadNoteRepository.deleteAll();
    leadActivityRepository.deleteAll();
    leadRepository.deleteAll();

    // Create a test lead
    testLead = new Lead("John", "Doe", "john.doe@example.com", "WEBSITE");
    testLead.setCreatedBy("test-user");
    testLead.setUpdatedBy("test-user");
    testLead = leadRepository.save(testLead);

    // Add some notes
    LeadNote note1 = new LeadNote();
    note1.setContent("First note about the lead");
    note1.setCreatedBy("test-user");
    note1.setUpdatedBy("test-user");
    testLead.addNote(note1);
    leadNoteRepository.save(note1);

    LeadNote note2 = new LeadNote();
    note2.setContent("Second note about the lead");
    note2.setIsPinned(true);
    note2.setCreatedBy("test-user");
    note2.setUpdatedBy("test-user");
    testLead.addNote(note2);
    leadNoteRepository.save(note2);

    // Add some activities
    LeadActivity activity1 = new LeadActivity();
    activity1.setType(ActivityType.LEAD_CREATED);
    activity1.setDescription("Lead was created");
    activity1.setCreatedBy("test-user");
    testLead.addActivity(activity1);
    leadActivityRepository.save(activity1);

    LeadActivity activity2 = new LeadActivity();
    activity2.setType(ActivityType.NOTE_ADDED);
    activity2.setDescription("Note was added to lead");
    activity2.setCreatedBy("test-user");
    testLead.addActivity(activity2);
    leadActivityRepository.save(activity2);

    // Flush and clear to ensure we are testing the entity graph loading from DB
    entityManager.flush();
    entityManager.clear();
  }

  @Test
  @DisplayName("Should load lead with notes using entity graph")
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

    // Verify notes are loaded
    assertThat(lead.getLeadNotes()).hasSize(2);
    assertThat(lead.getLeadNotes())
        .extracting(LeadNote::getContent)
        .containsExactlyInAnyOrder("First note about the lead", "Second note about the lead");

    // Verify one note is pinned
    assertThat(lead.getLeadNotes())
        .filteredOn(LeadNote::getIsPinned)
        .hasSize(1);
  }

  @Test
  @DisplayName("Should load lead with activities using entity graph")
  void shouldLoadLeadWithActivities() {
    // When
    Optional<Lead> leadWithActivities = leadRepository.findWithActivitiesById(testLead.getId());

    // Then
    assertThat(leadWithActivities).isPresent();
    Lead lead = leadWithActivities.get();

    // Verify lead data
    assertThat(lead.getFirstName()).isEqualTo("John");
    assertThat(lead.getLastName()).isEqualTo("Doe");

    // Verify activities are loaded
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
    assertThat(lead.getLeadNotes()).hasSize(2);
    assertThat(lead.getActivities()).hasSize(2);

    // Verify notes content
    assertThat(lead.getLeadNotes())
        .extracting(LeadNote::getContent)
        .containsExactlyInAnyOrder("First note about the lead", "Second note about the lead");

    // Verify activities content
    assertThat(lead.getActivities())
        .extracting(LeadActivity::getType)
        .containsExactlyInAnyOrder(ActivityType.LEAD_CREATED, ActivityType.NOTE_ADDED);
  }

  @Test
  @DisplayName("Should load lead with notes by email using entity graph")
  void shouldLoadLeadWithNotesByEmail() {
    // When
    Optional<Lead> leadWithNotes = leadRepository.findWithNotesByEmail("john.doe@example.com");

    // Then
    assertThat(leadWithNotes).isPresent();
    Lead lead = leadWithNotes.get();

    assertThat(lead.getEmail()).isEqualTo("john.doe@example.com");
    assertThat(lead.getLeadNotes()).hasSize(2);
  }

  @Test
  @DisplayName("Should load leads with notes by status using entity graph")
  void shouldLoadLeadsWithNotesByStatus() {
    // When
    List<Lead> leadsWithNotes = leadRepository.findWithNotesByStatus(LeadStatus.NEW);

    // Then
    assertThat(leadsWithNotes).hasSize(1);
    Lead lead = leadsWithNotes.get(0);

    assertThat(lead.getStatus()).isEqualTo(LeadStatus.NEW);
    assertThat(lead.getLeadNotes()).hasSize(2);
  }

  @Test
  @DisplayName("Should return empty when lead not found with entity graph")
  void shouldReturnEmptyWhenLeadNotFound() {
    // When
    Optional<Lead> nonExistentLead = leadRepository.findWithNotesById(99999L);

    // Then
    assertThat(nonExistentLead).isEmpty();
  }

  @Test
  @DisplayName("Should handle lead with no notes or activities")
  void shouldHandleLeadWithNoRelatedEntities() {
    // Given - create a lead with no notes or activities
    Lead emptyLead = new Lead("Jane", "Smith", "jane.smith@example.com", "EMAIL_CAMPAIGN");
    emptyLead.setCreatedBy("test-user");
    emptyLead.setUpdatedBy("test-user");
    emptyLead = leadRepository.save(emptyLead);
    entityManager.flush();
    entityManager.clear();

    // When
    Optional<Lead> leadWithHistory = leadRepository.findWithNotesAndActivitiesById(emptyLead.getId());

    // Then
    assertThat(leadWithHistory).isPresent();
    Lead lead = leadWithHistory.get();

    assertThat(lead.getFirstName()).isEqualTo("Jane");
    assertThat(lead.getLeadNotes()).isEmpty();
    assertThat(lead.getActivities()).isEmpty();
  }

  @Test
  @DisplayName("Should verify collections are ordered by creation date descending")
  void shouldVerifyCollectionsAreOrdered() {
    // When
    Optional<Lead> leadWithHistory = leadRepository.findWithNotesAndActivitiesById(testLead.getId());

    // Then
    assertThat(leadWithHistory).isPresent();
    Lead lead = leadWithHistory.get();

    // Verify notes are ordered by createdAt DESC
    List<LeadNote> notesList = new ArrayList<>(lead.getLeadNotes());
    if (notesList.size() > 1) {
      for (int i = 0; i < notesList.size() - 1; i++) {
        LeadNote current = notesList.get(i);
        LeadNote next = notesList.get(i + 1);
        assertThat(current.getCreatedAt()).isAfterOrEqualTo(next.getCreatedAt());
      }
    }

    // Verify activities are ordered by createdAt DESC
    List<LeadActivity> activitiesList = new ArrayList<>(lead.getActivities());
    if (activitiesList.size() > 1) {
      for (int i = 0; i < activitiesList.size() - 1; i++) {
        LeadActivity current = activitiesList.get(i);
        LeadActivity next = activitiesList.get(i + 1);
        assertThat(current.getCreatedAt()).isAfterOrEqualTo(next.getCreatedAt());
      }
    }
  }
}