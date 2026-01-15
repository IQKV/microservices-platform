package com.iqscaffold.contactservice.contact;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ContactService {

  Contact createContact(Contact contact);

  Optional<Contact> getContactById(Long id);

  Optional<Contact> getContactByEmail(String email);

  Page<Contact> getAllContacts(Pageable pageable);

  Page<Contact> getContactsByStatus(ContactStatus status, Pageable pageable);

  Page<Contact> searchContacts(String searchTerm, Pageable pageable);

  List<Contact> getContactsByCompany(Long companyId);

  Contact updateContact(Long id, Contact contact);

  void deleteContact(Long id);

  boolean existsByEmail(String email);

  long getContactCountByStatus(ContactStatus status);

  Contact updateLeadScore(Long id, Integer score);

  // Bulk operations
  List<Contact> bulkCreateContacts(List<Contact> contacts);

  Map<Long, Contact> bulkUpdateStatus(List<Long> contactIds, ContactStatus status, String userId);

  Map<Long, Boolean> bulkDeleteContacts(List<Long> contactIds);

  Map<Long, Contact> bulkUpdateLeadScores(Map<Long, Integer> contactScores);
}