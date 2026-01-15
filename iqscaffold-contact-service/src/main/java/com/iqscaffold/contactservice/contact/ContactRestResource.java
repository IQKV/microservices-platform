package com.iqscaffold.contactservice.contact;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

import com.iqscaffold.contactservice.contact.dto.ContactDtos;
import com.iqscaffold.contactservice.contact.dto.ContactMapper;
import com.iqscaffold.contactservice.shared.exception.ContactNotFoundException;
import com.iqscaffold.contactservice.shared.exception.DuplicateResourceException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for contact management operations.
 * <p>
 * Provides endpoints for:
 * <ul>
 *   <li>Creating new contacts</li>
 *   <li>Retrieving contact details</li>
 *   <li>Updating contact information</li>
 *   <li>Deleting contacts</li>
 *   <li>Searching and filtering contacts</li>
 * </ul>
 *
 * <h4>Authorization:</h4>
 * <ul>
 *   <li>Contact creation: Requires USER, ADMIN, or SUPER_ADMIN role</li>
 *   <li>Contact viewing: Requires USER, ADMIN, or SUPER_ADMIN role</li>
 *   <li>Contact updating: Requires USER, ADMIN, or SUPER_ADMIN role</li>
 *   <li>Contact deletion: Requires ADMIN or SUPER_ADMIN role</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/contacts")
@Tag(name = "Contacts", description = "Contact management operations")
@SecurityRequirement(name = "bearerAuth")
public class ContactRestResource {

  private final ContactService contactService;

  public ContactRestResource(final ContactService contactService) {
    this.contactService = contactService;
  }

  /**
   * Creates a new contact.
   *
   * @param request The contact creation request
   * @return The created contact
   */
  @Operation(
      summary = "Create contact",
      description = "Creates a new contact with the provided information")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Contact created successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "409", description = "Contact with email already exists")
  })
  @PostMapping
  @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ContactDtos.ContactResponse> createContact(
      @Valid @RequestBody ContactDtos.CreateContactRequest request) {
    String userId = getCurrentUserId();

    // Check for duplicate email if provided
    if (request.email() != null && !request.email().isBlank()) {
      if (contactService.existsByEmail(request.email())) {
        throw new DuplicateResourceException("Contact with email already exists: " + request.email());
      }
    }

    Contact contact = ContactMapper.toEntity(request, userId);
    Contact savedContact = contactService.createContact(contact);
    ContactDtos.ContactResponse response = ContactMapper.toResponse(savedContact);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Retrieves a contact by ID.
   *
   * @param id The contact ID
   * @return The contact details
   */
  @Operation(
      summary = "Get contact by ID",
      description = "Retrieves a specific contact by its ID")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Contact found"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Contact not found")
  })
  @GetMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ContactDtos.ContactResponse> getContactById(@PathVariable Long id) {
    Contact contact = contactService.getContactById(id)
        .orElseThrow(() -> new ContactNotFoundException("Contact not found with id: " + id));
    ContactDtos.ContactResponse response = ContactMapper.toResponse(contact);
    return ResponseEntity.ok(response);
  }

  /**
   * Lists contacts with optional search and filtering.
   *
   * @param search   Search term to match against name or email
   * @param status   Filter by contact status
   * @param pageable Pagination parameters (page, size, sort)
   * @return Paginated list of contacts
   */
  @Operation(
      summary = "List contacts",
      description = "Retrieves a paginated list of contacts with optional search and filtering. "
                    + "Search term matches against first name, last name, and email.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Contacts retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  @GetMapping
  @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<Page<ContactDtos.ContactResponse>> listContacts(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) ContactStatus status,
      Pageable pageable) {

    Page<Contact> contacts;

    if (search != null && !search.isBlank()) {
      contacts = contactService.searchContacts(search, pageable);
    } else if (status != null) {
      contacts = contactService.getContactsByStatus(status, pageable);
    } else {
      contacts = contactService.getAllContacts(pageable);
    }

    Page<ContactDtos.ContactResponse> response = contacts.map(ContactMapper::toResponse);
    return ResponseEntity.ok(response);
  }

  /**
   * Updates an existing contact.
   *
   * @param id      The contact ID
   * @param request The contact update request
   * @return The updated contact
   */
  @Operation(
      summary = "Update contact",
      description = "Updates an existing contact with the provided information")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Contact updated successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Contact not found"),
      @ApiResponse(responseCode = "409", description = "Contact with email already exists")
  })
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ContactDtos.ContactResponse> updateContact(
      @PathVariable Long id,
      @Valid @RequestBody ContactDtos.UpdateContactRequest request) {
    String userId = getCurrentUserId();

    // Check if contact exists
    Contact existingContact = contactService.getContactById(id)
        .orElseThrow(() -> new ContactNotFoundException("Contact not found with id: " + id));

    // Check for duplicate email if email is being changed
    if (request.email() != null && !request.email().isBlank()) {
      if (!request.email().equals(existingContact.getEmail())) {
        if (contactService.existsByEmail(request.email())) {
          throw new DuplicateResourceException("Contact with email already exists: " + request.email());
        }
      }
    }

    ContactMapper.updateEntity(existingContact, request, userId);
    Contact updatedContact = contactService.updateContact(id, existingContact);
    ContactDtos.ContactResponse response = ContactMapper.toResponse(updatedContact);

    return ResponseEntity.ok(response);
  }

  /**
   * Deletes a contact.
   *
   * @param id The contact ID
   * @return No content
   */
  @Operation(
      summary = "Delete contact",
      description = "Deletes a contact by its ID")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Contact deleted successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or SUPER_ADMIN role"),
      @ApiResponse(responseCode = "404", description = "Contact not found")
  })
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<Void> deleteContact(@PathVariable Long id) {
    contactService.deleteContact(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Gets contacts by company ID.
   *
   * @param companyId The company ID
   * @return List of contacts for the company
   */
  @Operation(
      summary = "Get contacts by company",
      description = "Retrieves all contacts associated with a specific company")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Contacts retrieved successfully"),
      @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  @GetMapping("/company/{companyId}")
  @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<List<ContactDtos.ContactResponse>> getContactsByCompany(
      @PathVariable Long companyId) {
    List<Contact> contacts = contactService.getContactsByCompany(companyId);
    List<ContactDtos.ContactResponse> response = contacts.stream()
        .map(ContactMapper::toResponse)
        .collect(Collectors.toList());
    return ResponseEntity.ok(response);
  }

  /**
   * Updates the lead score for a contact.
   *
   * @param id      The contact ID
   * @param request The lead score update request
   * @return The updated contact
   */
  @Operation(
      summary = "Update contact lead score",
      description = "Updates the lead score (0-100) for a specific contact")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Lead score updated successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid score value"),
      @ApiResponse(responseCode = "401", description = "Unauthorized"),
      @ApiResponse(responseCode = "404", description = "Contact not found")
  })
  @PatchMapping("/{id}/score")
  @PreAuthorize("hasAnyAuthority('USER', 'ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<ContactDtos.ContactResponse> updateLeadScore(
      @PathVariable Long id,
      @Valid @RequestBody ContactDtos.UpdateLeadScoreRequest request) {
    Contact updatedContact = contactService.updateLeadScore(id, request.score());
    ContactDtos.ContactResponse response = ContactMapper.toResponse(updatedContact);
    return ResponseEntity.ok(response);
  }

  /**
   * Extracts the current user ID from the JWT token.
   *
   * @return The user ID, or "system" if not available
   */
  private String getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      String userId = jwt.getClaimAsString("userId");
      if (userId != null) {
        return userId;
      }
      // Fallback to subject if userId claim not present
      return jwt.getSubject();
    }
    return "system";
  }
}
