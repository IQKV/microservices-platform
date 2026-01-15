package com.iqscaffold.contactservice.contact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.iqscaffold.contactservice.event.ContactEventPublisher;
import com.iqscaffold.contactservice.shared.exception.ContactNotFoundException;
import com.iqscaffold.contactservice.webhook.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ContactServiceImpl implements ContactService {

  private static final Logger log = LoggerFactory.getLogger(ContactServiceImpl.class);

  private final ContactRepository contactRepository;
  private final ContactEventPublisher eventPublisher;
  private final WebhookService webhookService;

  public ContactServiceImpl(
      final ContactRepository contactRepository,
      final ContactEventPublisher eventPublisher,
      final WebhookService webhookService) {
    this.contactRepository = contactRepository;
    this.eventPublisher = eventPublisher;
    this.webhookService = webhookService;
  }

  @Override
  public Contact createContact(Contact contact) {
    Contact savedContact = contactRepository.save(contact);
    // Publish contact created event
    eventPublisher.publishContactCreated(savedContact);
    // Trigger webhooks
    webhookService.triggerWebhooks("contact.created", savedContact);
    return savedContact;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Contact> getContactById(Long id) {
    return contactRepository.findById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Contact> getContactByEmail(String email) {
    return contactRepository.findByEmail(email);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Contact> getAllContacts(Pageable pageable) {
    return contactRepository.findAll(pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Contact> getContactsByStatus(ContactStatus status, Pageable pageable) {
    return contactRepository.findByStatus(status, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Contact> searchContacts(String searchTerm, Pageable pageable) {
    return contactRepository.searchContacts(searchTerm, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Contact> getContactsByCompany(Long companyId) {
    return contactRepository.findByCompanyId(companyId);
  }

  @Override
  public Contact updateContact(Long id, Contact contact) {
    Contact existingContact = contactRepository.findById(id)
        .orElseThrow(() -> new ContactNotFoundException("Contact not found with id: " + id));

    existingContact.setFirstName(contact.getFirstName());
    existingContact.setLastName(contact.getLastName());
    existingContact.setEmail(contact.getEmail());
    existingContact.setPhone(contact.getPhone());
    existingContact.setJobTitle(contact.getJobTitle());
    existingContact.setCompanyId(contact.getCompanyId());
    existingContact.setStatus(contact.getStatus());
    existingContact.setNotes(contact.getNotes());
    existingContact.setUpdatedBy(contact.getUpdatedBy());

    Contact updatedContact = contactRepository.save(existingContact);
    // Publish contact updated event
    eventPublisher.publishContactUpdated(updatedContact);
    // Trigger webhooks
    webhookService.triggerWebhooks("contact.updated", updatedContact);
    return updatedContact;
  }

  @Override
  public void deleteContact(Long id) {
    Contact contact = contactRepository.findById(id)
        .orElseThrow(() -> new ContactNotFoundException("Contact not found with id: " + id));
    
    String email = contact.getEmail();
    contactRepository.deleteById(id);
    
    // Publish contact deleted event
    eventPublisher.publishContactDeleted(id, email);
    // Trigger webhooks
    webhookService.triggerWebhooks("contact.deleted", contact);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByEmail(String email) {
    return contactRepository.existsByEmail(email);
  }

  @Override
  @Transactional(readOnly = true)
  public long getContactCountByStatus(ContactStatus status) {
    return contactRepository.countByStatus(status);
  }

  @Override
  public Contact updateLeadScore(Long id, Integer score) {
    Contact contact = contactRepository.findById(id)
        .orElseThrow(() -> new ContactNotFoundException("Contact not found with id: " + id));

    contact.setLeadScore(score);
    Contact updatedContact = contactRepository.save(contact);
    // Publish contact updated event
    eventPublisher.publishContactUpdated(updatedContact);
    return updatedContact;
  }

  @Override
  public List<Contact> bulkCreateContacts(List<Contact> contacts) {
    log.info("Bulk creating {} contacts", contacts.size());
    List<Contact> savedContacts = new ArrayList<>();

    for (final Contact contact : contacts) {
      try {
        Contact savedContact = contactRepository.save(contact);
        savedContacts.add(savedContact);
        // Publish contact created event
        eventPublisher.publishContactCreated(savedContact);
      } catch (final Exception e) {
        log.error("Failed to create contact: {}", contact.getEmail(), e);
        // Continue with next contact
      }
    }

    log.info("Successfully created {} out of {} contacts", savedContacts.size(), contacts.size());
    return savedContacts;
  }

  @Override
  public Map<Long, Contact> bulkUpdateStatus(
      List<Long> contactIds,
      ContactStatus status,
      String userId) {
    log.info("Bulk updating status to {} for {} contacts", status, contactIds.size());
    Map<Long, Contact> updatedContacts = new HashMap<>();

    for (final Long contactId : contactIds) {
      try {
        Optional<Contact> optionalContact = contactRepository.findById(contactId);
        if (optionalContact.isPresent()) {
          Contact contact = optionalContact.get();
          contact.setStatus(status);
          contact.setUpdatedBy(userId);
          Contact updatedContact = contactRepository.save(contact);
          updatedContacts.put(contactId, updatedContact);
          // Publish contact updated event
          eventPublisher.publishContactUpdated(updatedContact);
        } else {
          log.warn("Contact not found with id: {}", contactId);
        }
      } catch (final Exception e) {
        log.error("Failed to update status for contact: {}", contactId, e);
        // Continue with next contact
      }
    }

    log.info("Successfully updated {} out of {} contacts", updatedContacts.size(), contactIds.size());
    return updatedContacts;
  }

  @Override
  public Map<Long, Boolean> bulkDeleteContacts(List<Long> contactIds) {
    log.info("Bulk deleting {} contacts", contactIds.size());
    Map<Long, Boolean> results = new HashMap<>();

    for (final Long contactId : contactIds) {
      try {
        Optional<Contact> optionalContact = contactRepository.findById(contactId);
        if (optionalContact.isPresent()) {
          Contact contact = optionalContact.get();
          String email = contact.getEmail();
          contactRepository.deleteById(contactId);
          results.put(contactId, true);
          // Publish contact deleted event
          eventPublisher.publishContactDeleted(contactId, email);
        } else {
          log.warn("Contact not found with id: {}", contactId);
          results.put(contactId, false);
        }
      } catch (final Exception e) {
        log.error("Failed to delete contact: {}", contactId, e);
        results.put(contactId, false);
      }
    }

    long successCount = results.values().stream().filter(Boolean::booleanValue).count();
    log.info("Successfully deleted {} out of {} contacts", successCount, contactIds.size());
    return results;
  }

  @Override
  public Map<Long, Contact> bulkUpdateLeadScores(Map<Long, Integer> contactScores) {
    log.info("Bulk updating lead scores for {} contacts", contactScores.size());
    Map<Long, Contact> updatedContacts = new HashMap<>();

    for (final Map.Entry<Long, Integer> entry : contactScores.entrySet()) {
      Long contactId = entry.getKey();
      Integer score = entry.getValue();

      try {
        Optional<Contact> optionalContact = contactRepository.findById(contactId);
        if (optionalContact.isPresent()) {
          Contact contact = optionalContact.get();
          contact.setLeadScore(score);
          Contact updatedContact = contactRepository.save(contact);
          updatedContacts.put(contactId, updatedContact);
          // Publish contact updated event
          eventPublisher.publishContactUpdated(updatedContact);
        } else {
          log.warn("Contact not found with id: {}", contactId);
        }
      } catch (final Exception e) {
        log.error("Failed to update lead score for contact: {}", contactId, e);
        // Continue with next contact
      }
    }

    log.info("Successfully updated {} out of {} contacts", updatedContacts.size(), contactScores.size());
    return updatedContacts;
  }
}