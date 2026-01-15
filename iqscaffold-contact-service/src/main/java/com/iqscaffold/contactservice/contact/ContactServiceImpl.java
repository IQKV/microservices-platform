package com.iqscaffold.contactservice.contact;

import java.util.List;
import java.util.Optional;

import com.iqscaffold.contactservice.event.ContactEventPublisher;
import com.iqscaffold.contactservice.shared.exception.ContactNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ContactServiceImpl implements ContactService {

  private final ContactRepository contactRepository;
  private final ContactEventPublisher eventPublisher;

  public ContactServiceImpl(
      final ContactRepository contactRepository,
      final ContactEventPublisher eventPublisher) {
    this.contactRepository = contactRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public Contact createContact(Contact contact) {
    Contact savedContact = contactRepository.save(contact);
    // Publish contact created event
    eventPublisher.publishContactCreated(savedContact);
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
}