package com.iqscaffold.userservice.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.iqscaffold.userservice.usermanagement.User;
import com.iqscaffold.userservice.usermanagement.UserRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class UserAuditLogRepositoryTest {

  @Autowired
  private UserAuditLogRepository repository;

  @Autowired
  private UserRepository userRepository;

  @Test
  @DisplayName("findByUserIdOrderByCreatedAtDesc returns newest first for a user")
  void byUserOrderedDesc() throws InterruptedException {
    var t = "TEN";
    com.iqscaffold.userservice.tenancy.TenantContext.setCurrentTenantId(t);
    var u = userRepository.save(new User("u1", "u1@x.com", "h", "F", "L", t));
    repository.save(new UserAuditLog(u.getId(), "LOGIN_SUCCESS", t));
    Thread.sleep(5);
    repository.save(new UserAuditLog(u.getId(), "LOGOUT", t));

    Page<UserAuditLog> page = repository.findByUserIdOrderByCreatedAtDesc(u.getId(), PageRequest.of(0, 10));
    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.getContent().get(0).getAction()).isEqualTo("LOGOUT");
  }

  @Test
  @DisplayName("findByTenantIdOrderByCreatedAtDesc filters by tenant and orders desc")
  void byTenantOrderedDesc() {
    com.iqscaffold.userservice.tenancy.TenantContext.setCurrentTenantId("A");
    var userA1 = userRepository.save(new User("a1", "a1@x.com", "h", "F", "L", "A"));
    var userA2 = userRepository.save(new User("a2", "a2@x.com", "h", "F", "L", "A"));

    repository.saveAll(List.of(
        new UserAuditLog(userA1.getId(), "LOGIN_SUCCESS", "A"),
        new UserAuditLog(userA2.getId(), "LOGIN_FAILURE", "A")
    ));

    var page = repository.findAllOrderByCreatedAtDesc(PageRequest.of(0, 10));
    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.getContent()).allMatch(l -> "A".equals(l.getTenantId()));
  }

  @Test
  @DisplayName("findByActionOrderByCreatedAtDesc filters by action")
  void byAction() {
    com.iqscaffold.userservice.tenancy.TenantContext.setCurrentTenantId("T");
    var u1 = userRepository.save(new User("t1", "t1@x.com", "h", "F", "L", "T"));
    var u2 = userRepository.save(new User("t2", "t2@x.com", "h", "F", "L", "T"));
    var u3 = userRepository.save(new User("t3", "t3@x.com", "h", "F", "L", "T"));
    repository.saveAll(List.of(
        new UserAuditLog(u1.getId(), "LOGIN_SUCCESS", "T"),
        new UserAuditLog(u2.getId(), "LOGIN_FAILURE", "T"),
        new UserAuditLog(u3.getId(), "LOGIN_SUCCESS", "T")
    ));

    var page = repository.findByActionOrderByCreatedAtDesc("LOGIN_SUCCESS", PageRequest.of(0, 10));
    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.getContent()).allMatch(l -> "LOGIN_SUCCESS".equals(l.getAction()));
  }

  @Test
  @DisplayName("findByTenantIdAndDateRange includes records within window")
  void byDateRange() {
    var tenant = "TX";
    com.iqscaffold.userservice.tenancy.TenantContext.setCurrentTenantId(tenant);
    var start = Instant.now().minusSeconds(60);
    var u = userRepository.save(new User("x1", "x1@x.com", "h", "F", "L", tenant));
    repository.save(new UserAuditLog(u.getId(), "LOGIN_SUCCESS", tenant));
    var end = Instant.now().plusSeconds(60);

    var page = repository.findByDateRange(start, end, PageRequest.of(0, 10));
    assertThat(page.getTotalElements()).isEqualTo(1);
  }

  @Test
  @DisplayName("findSecurityAuditLogsByTenantId matches security related actions")
  void securityLogs() {
    com.iqscaffold.userservice.tenancy.TenantContext.setCurrentTenantId("TS");
    var u = userRepository.save(new User("s1", "s1@x.com", "h", "F", "L", "TS"));
    repository.saveAll(List.of(
        new UserAuditLog(u.getId(), "LOGIN_SUCCESS", "TS"),
        new UserAuditLog(u.getId(), "PASSWORD_CHANGED", "TS"),
        new UserAuditLog(u.getId(), "ACCOUNT_LOCKED", "TS"),
        new UserAuditLog(u.getId(), "OTHER", "TS")
    ));

    var page = repository.findSecurityAuditLogs(PageRequest.of(0, 10));
    assertThat(page.getContent())
        .extracting(UserAuditLog::getAction)
        .contains("LOGIN_SUCCESS", "PASSWORD_CHANGED", "ACCOUNT_LOCKED")
        .doesNotContain("OTHER");
  }

  @Test
  @DisplayName("findFailedLoginAttempts returns failures since time and countByTenantIdAndAction counts by action")
  void failedLoginsAndCount() {
    var tenant = "TCOUNT";
    com.iqscaffold.userservice.tenancy.TenantContext.setCurrentTenantId(tenant);
    var u = userRepository.save(new User("c1", "c1@x.com", "h", "F", "L", tenant));
    repository.saveAll(List.of(
        new UserAuditLog(u.getId(), "LOGIN_FAILURE", tenant),
        new UserAuditLog(u.getId(), "LOGIN_FAILURE", tenant),
        new UserAuditLog(u.getId(), "LOGIN_SUCCESS", tenant)
    ));

    var since = Instant.now().minusSeconds(3600);
    var failures = repository.findFailedLoginAttempts(u.getId(), since);
    assertThat(failures).hasSize(2);

    var countLoginSuccess = repository.countByAction("LOGIN_SUCCESS");
    assertThat(countLoginSuccess).isEqualTo(1);
  }
}
