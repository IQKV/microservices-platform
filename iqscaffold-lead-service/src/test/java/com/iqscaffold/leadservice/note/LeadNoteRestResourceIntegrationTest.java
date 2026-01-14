package com.iqscaffold.leadservice.note;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqscaffold.leadservice.lead.Lead;
import com.iqscaffold.leadservice.lead.LeadRepository;
import com.iqscaffold.leadservice.note.dto.LeadNoteDtos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for Lead Notes operations.
 * Tests Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LeadNoteRestResourceIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private LeadRepository leadRepository;

  @Autowired
  private LeadNoteRepository leadNoteRepository;

  private Lead testLead;

  @BeforeEach
  void setUp() {
    leadNoteRepository.deleteAll();
    leadRepository.deleteAll();

    // Create a test lead for note operations
    testLead = new Lead("John", "Doe", "john.doe@example.com", "Website");
    testLead.setPhone("+1234567890");
    testLead.setCompany("Test Company");
    testLead.setCreatedBy("test-user");
    testLead.setUpdatedBy("test-user");
    testLead = leadRepository.save(testLead);
  }

  /**
   * Test add note to lead.
   * Requirement 6.1: WHEN a user adds a note to a lead with content,
   * THE CRM_System SHALL create the note with a timestamp and author information
   */
  @Test
  @DisplayName("Should add note to lead with valid content")
  @WithMockUser(authorities = {"USER"})
  void testAddNoteToLead() throws Exception {
    // Given
    LeadNoteDtos.CreateLeadNoteRequest request = new LeadNoteDtos.CreateLeadNoteRequest(
        "This is a test note about the lead",
        false
    );

    // When & Then
    mockMvc.perform(post("/api/v1/leads/{leadId}/notes", testLead.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.leadId", is(testLead.getId().intValue())))
        .andExpect(jsonPath("$.content", is("This is a test note about the lead")))
        .andExpect(jsonPath("$.isPinned", is(false)))
        .andExpect(jsonPath("$.createdAt", notNullValue()))
        .andExpect(jsonPath("$.updatedAt", notNullValue()))
        .andExpect(jsonPath("$.createdBy", notNullValue()))
        .andExpect(jsonPath("$.updatedBy", notNullValue()));
  }

  /**
   * Test add note with pinned flag.
   * Requirement 6.1: Note should support pinned flag
   */
  @Test
  @DisplayName("Should add pinned note to lead")
  @WithMockUser(authorities = {"USER"})
  void testAddPinnedNoteToLead() throws Exception {
    // Given
    LeadNoteDtos.CreateLeadNoteRequest request = new LeadNoteDtos.CreateLeadNoteRequest(
        "This is an important pinned note",
        true
    );

    // When & Then
    mockMvc.perform(post("/api/v1/leads/{leadId}/notes", testLead.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.content", is("This is an important pinned note")))
        .andExpect(jsonPath("$.isPinned", is(true)));
  }

  /**
   * Test add note with empty content.
   * Requirement 6.5: WHEN a user creates a note with empty content,
   * THE CRM_System SHALL reject the request with a validation error
   */
  @Test
  @DisplayName("Should reject note with empty content")
  @WithMockUser(authorities = {"USER"})
  void testAddNoteWithEmptyContent() throws Exception {
    // Given
    LeadNoteDtos.CreateLeadNoteRequest request = new LeadNoteDtos.CreateLeadNoteRequest(
        "",
        false
    );

    // When & Then
    mockMvc.perform(post("/api/v1/leads/{leadId}/notes", testLead.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  /**
   * Test add note with blank content (whitespace only).
   * Requirement 6.5: Blank content should be rejected
   */
  @Test
  @DisplayName("Should reject note with blank content")
  @WithMockUser(authorities = {"USER"})
  void testAddNoteWithBlankContent() throws Exception {
    // Given
    LeadNoteDtos.CreateLeadNoteRequest request = new LeadNoteDtos.CreateLeadNoteRequest(
        "   ",
        false
    );

    // When & Then
    mockMvc.perform(post("/api/v1/leads/{leadId}/notes", testLead.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  /**
   * Test add note to non-existent lead.
   * Requirement 6.6: WHEN a user creates a note for a non-existent lead,
   * THE CRM_System SHALL reject the request with a not found error
   */
  @Test
  @DisplayName("Should return 404 when adding note to non-existent lead")
  @WithMockUser(authorities = {"USER"})
  void testAddNoteToNonExistentLead() throws Exception {
    // Given
    LeadNoteDtos.CreateLeadNoteRequest request = new LeadNoteDtos.CreateLeadNoteRequest(
        "This note is for a non-existent lead",
        false
    );

    // When & Then
    mockMvc.perform(post("/api/v1/leads/{leadId}/notes", 99999L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());
  }

  /**
   * Test get notes for lead.
   * Requirement 6.2: WHEN a user retrieves notes for a lead,
   * THE CRM_System SHALL return all notes sorted by creation date descending
   */
  @Test
  @DisplayName("Should get all notes for a lead sorted by creation date descending")
  @WithMockUser(authorities = {"USER"})
  void testGetLeadNotes() throws Exception {
    // Given - Create multiple notes for the lead
    LeadNote note1 = new LeadNote(testLead, "First note", "test-user");
    LeadNote note2 = new LeadNote(testLead, "Second note", "test-user");
    LeadNote note3 = new LeadNote(testLead, "Third note", "test-user");

    leadNoteRepository.save(note1);
    // Add a small delay to ensure different timestamps
    Thread.sleep(10);
    leadNoteRepository.save(note2);
    Thread.sleep(10);
    leadNoteRepository.save(note3);

    // When & Then
    mockMvc.perform(get("/api/v1/leads/{leadId}/notes", testLead.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(jsonPath("$[0].content", is("Third note")))  // Most recent first
        .andExpect(jsonPath("$[1].content", is("Second note")))
        .andExpect(jsonPath("$[2].content", is("First note")));
  }

  /**
   * Test get notes for lead with no notes.
   * Requirement 6.2: Should return empty list when lead has no notes
   */
  @Test
  @DisplayName("Should return empty list when lead has no notes")
  @WithMockUser(authorities = {"USER"})
  void testGetLeadNotesEmpty() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/v1/leads/{leadId}/notes", testLead.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  /**
   * Test get notes for non-existent lead.
   * Requirement 6.6: Should return 404 when lead doesn't exist
   */
  @Test
  @DisplayName("Should return 404 when getting notes for non-existent lead")
  @WithMockUser(authorities = {"USER"})
  void testGetNotesForNonExistentLead() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/v1/leads/{leadId}/notes", 99999L)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  /**
   * Test update note content.
   * Requirement 6.3: WHEN a user updates a note's content,
   * THE CRM_System SHALL save the changes and update the modification timestamp
   */
  @Test
  @DisplayName("Should update note content successfully")
  @WithMockUser(authorities = {"USER"})
  void testUpdateNote() throws Exception {
    // Given - Create a note first
    LeadNote note = new LeadNote(testLead, "Original content", "test-user");
    LeadNote savedNote = leadNoteRepository.save(note);

    // Update request
    LeadNoteDtos.UpdateLeadNoteRequest updateRequest = new LeadNoteDtos.UpdateLeadNoteRequest(
        "Updated content",
        false
    );

    // When & Then
    mockMvc.perform(put("/api/v1/leads/{leadId}/notes/{noteId}", testLead.getId(), savedNote.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(savedNote.getId().intValue())))
        .andExpect(jsonPath("$.content", is("Updated content")))
        .andExpect(jsonPath("$.updatedAt", notNullValue()));
  }

  /**
   * Test update note pinned status.
   * Requirement 6.3: Should be able to update pinned status
   */
  @Test
  @DisplayName("Should update note pinned status")
  @WithMockUser(authorities = {"USER"})
  void testUpdateNotePinnedStatus() throws Exception {
    // Given - Create an unpinned note
    LeadNote note = new LeadNote(testLead, "Test note", "test-user");
    note.setIsPinned(false);
    LeadNote savedNote = leadNoteRepository.save(note);

    // Update request to pin the note
    LeadNoteDtos.UpdateLeadNoteRequest updateRequest = new LeadNoteDtos.UpdateLeadNoteRequest(
        "Test note",
        true
    );

    // When & Then
    mockMvc.perform(put("/api/v1/leads/{leadId}/notes/{noteId}", testLead.getId(), savedNote.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(savedNote.getId().intValue())))
        .andExpect(jsonPath("$.isPinned", is(true)));
  }

  /**
   * Test update note with empty content.
   * Requirement 6.5: Should reject update with empty content
   */
  @Test
  @DisplayName("Should reject update with empty content")
  @WithMockUser(authorities = {"USER"})
  void testUpdateNoteWithEmptyContent() throws Exception {
    // Given - Create a note first
    LeadNote note = new LeadNote(testLead, "Original content", "test-user");
    LeadNote savedNote = leadNoteRepository.save(note);

    // Update request with empty content
    LeadNoteDtos.UpdateLeadNoteRequest updateRequest = new LeadNoteDtos.UpdateLeadNoteRequest(
        "",
        false
    );

    // When & Then
    mockMvc.perform(put("/api/v1/leads/{leadId}/notes/{noteId}", testLead.getId(), savedNote.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isBadRequest());
  }

  /**
   * Test update non-existent note.
   * Requirement 6.3: Should return 404 when updating non-existent note
   */
  @Test
  @DisplayName("Should return 404 when updating non-existent note")
  @WithMockUser(authorities = {"USER"})
  void testUpdateNonExistentNote() throws Exception {
    // Given
    LeadNoteDtos.UpdateLeadNoteRequest updateRequest = new LeadNoteDtos.UpdateLeadNoteRequest(
        "Updated content",
        false
    );

    // When & Then
    mockMvc.perform(put("/api/v1/leads/{leadId}/notes/{noteId}", testLead.getId(), 99999L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isNotFound());
  }

  /**
   * Test update note with wrong lead ID.
   * Requirement 6.3: Should return 404 when lead ID doesn't match
   */
  @Test
  @DisplayName("Should return 404 when updating note with wrong lead ID")
  @WithMockUser(authorities = {"USER"})
  void testUpdateNoteWithWrongLeadId() throws Exception {
    // Given - Create a note for testLead
    LeadNote note = new LeadNote(testLead, "Original content", "test-user");
    LeadNote savedNote = leadNoteRepository.save(note);

    // Update request
    LeadNoteDtos.UpdateLeadNoteRequest updateRequest = new LeadNoteDtos.UpdateLeadNoteRequest(
        "Updated content",
        false
    );

    // When & Then - Try to update with wrong lead ID
    mockMvc.perform(put("/api/v1/leads/{leadId}/notes/{noteId}", 99999L, savedNote.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isNotFound());
  }

  /**
   * Test delete note.
   * Requirement 6.4: WHEN a user deletes a note,
   * THE CRM_System SHALL remove it from the database
   */
  @Test
  @DisplayName("Should delete note successfully")
  @WithMockUser(authorities = {"USER"})
  void testDeleteNote() throws Exception {
    // Given - Create a note first
    LeadNote note = new LeadNote(testLead, "Note to be deleted", "test-user");
    LeadNote savedNote = leadNoteRepository.save(note);

    // When & Then - Delete the note
    mockMvc.perform(delete("/api/v1/leads/{leadId}/notes/{noteId}", testLead.getId(), savedNote.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    // Verify note is deleted by trying to get notes for the lead
    mockMvc.perform(get("/api/v1/leads/{leadId}/notes", testLead.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  /**
   * Test delete non-existent note.
   * Requirement 6.4: Should return 404 when deleting non-existent note
   */
  @Test
  @DisplayName("Should return 404 when deleting non-existent note")
  @WithMockUser(authorities = {"USER"})
  void testDeleteNonExistentNote() throws Exception {
    // When & Then
    mockMvc.perform(delete("/api/v1/leads/{leadId}/notes/{noteId}", testLead.getId(), 99999L)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  /**
   * Test delete note with wrong lead ID.
   * Requirement 6.4: Should return 404 when lead ID doesn't match
   */
  @Test
  @DisplayName("Should return 404 when deleting note with wrong lead ID")
  @WithMockUser(authorities = {"USER"})
  void testDeleteNoteWithWrongLeadId() throws Exception {
    // Given - Create a note for testLead
    LeadNote note = new LeadNote(testLead, "Note to be deleted", "test-user");
    LeadNote savedNote = leadNoteRepository.save(note);

    // When & Then - Try to delete with wrong lead ID
    mockMvc.perform(delete("/api/v1/leads/{leadId}/notes/{noteId}", 99999L, savedNote.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  /**
   * Test multiple notes for same lead.
   * Requirement 6.2: Should handle multiple notes for the same lead
   */
  @Test
  @DisplayName("Should handle multiple notes for the same lead")
  @WithMockUser(authorities = {"USER"})
  void testMultipleNotesForSameLead() throws Exception {
    // Given - Create multiple notes
    LeadNoteDtos.CreateLeadNoteRequest request1 = new LeadNoteDtos.CreateLeadNoteRequest(
        "First note",
        false
    );
    LeadNoteDtos.CreateLeadNoteRequest request2 = new LeadNoteDtos.CreateLeadNoteRequest(
        "Second note",
        true
    );
    LeadNoteDtos.CreateLeadNoteRequest request3 = new LeadNoteDtos.CreateLeadNoteRequest(
        "Third note",
        false
    );

    // When - Add three notes
    mockMvc.perform(post("/api/v1/leads/{leadId}/notes", testLead.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request1)))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/v1/leads/{leadId}/notes", testLead.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request2)))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/v1/leads/{leadId}/notes", testLead.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request3)))
        .andExpect(status().isCreated());

    // Then - Verify all notes are returned
    mockMvc.perform(get("/api/v1/leads/{leadId}/notes", testLead.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)));
  }

  /**
   * Test note content length validation.
   * Requirement 6.5: Note content should not exceed 5000 characters
   */
  @Test
  @DisplayName("Should reject note with content exceeding 5000 characters")
  @WithMockUser(authorities = {"USER"})
  void testNoteContentLengthValidation() throws Exception {
    // Given - Create a note with content exceeding 5000 characters
    String longContent = "a".repeat(5001);
    LeadNoteDtos.CreateLeadNoteRequest request = new LeadNoteDtos.CreateLeadNoteRequest(
        longContent,
        false
    );

    // When & Then
    mockMvc.perform(post("/api/v1/leads/{leadId}/notes", testLead.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  /**
   * Test authorization for note operations.
   * Requirement 6.1-6.4: All note operations require authentication
   */
  @Test
  @DisplayName("Should require authentication for note operations")
  void testAuthenticationRequired() throws Exception {
    // Given
    LeadNoteDtos.CreateLeadNoteRequest request = new LeadNoteDtos.CreateLeadNoteRequest(
        "Test note",
        false
    );

    // When & Then - Without authentication
    mockMvc.perform(post("/api/v1/leads/{leadId}/notes", testLead.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(get("/api/v1/leads/{leadId}/notes", testLead.getId())
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }
}
