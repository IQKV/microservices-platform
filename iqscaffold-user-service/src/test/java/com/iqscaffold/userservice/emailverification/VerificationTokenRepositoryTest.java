package com.iqscaffold.userservice.emailverification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import com.iqscaffold.userservice.tenancy.SchemaNameResolver;
import com.iqscaffold.userservice.tenancy.SchemaTenantIdentifierResolver;
import com.iqscaffold.userservice.tenancy.TenantContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(VerificationTokenRepositoryTest.MultiTenantTestConfig.class)
class VerificationTokenRepositoryTest {

  @Autowired
  private VerificationTokenRepository repository;

  @Test
  @DisplayName("findByTokenAndUsedFalse and existsByTokenAndValidAt should respect used and expiry")
  void findByTokenAndValidity() {
    var now = LocalDateTime.now();
    repository.save(new VerificationToken("tok-valid", 1L, now.plusHours(2), "t1"));
    var used = repository.save(new VerificationToken("tok-used", 1L, now.plusHours(2), "t1"));
    used.markAsUsed();
    repository.save(used);
    repository.save(new VerificationToken("tok-expired", 1L, now.minusMinutes(1), "t1"));

    assertThat(repository.findByTokenAndUsedFalse("tok-valid")).isPresent();
    assertThat(repository.findByTokenAndUsedFalse("tok-used")).isEmpty();

    assertThat(repository.existsByTokenAndValidAt("tok-valid", now)).isTrue();
    assertThat(repository.existsByTokenAndValidAt("tok-expired", now)).isFalse();
  }

  @Test
  @DisplayName("findByUserIdAndUsedFalse and countUnusedTokensByUserIdAndTenantId should return only unused")
  void unusedByUser() {
    var now = LocalDateTime.now();
    TenantContext.setCurrentTenantId("tA");
    repository.save(new VerificationToken("a1", 10L, now.plusHours(1), "tA"));
    var used = repository.save(new VerificationToken("a2", 10L, now.plusHours(1), "tA"));
    used.markAsUsed();
    repository.save(used);

    var list = repository.findByUserIdAndUsedFalse(10L);
    assertThat(list).extracting(VerificationToken::getToken).containsExactly("a1");

    var count = repository.countUnusedTokensByUserId(10L);
    assertThat(count).isEqualTo(1);
  }

  @Test
  @DisplayName("findByUserId and findUnusedTokensByUserId should order by createdAt desc within schema")
  void byTenantAndOrdering() throws InterruptedException {
    var now = LocalDateTime.now();
    TenantContext.setCurrentTenantId("tenantX");
    repository.save(new VerificationToken("t-1", 20L, now.plusHours(2), "tenantX"));
    Thread.sleep(5); // ensure createdAt ordering difference
    repository.save(new VerificationToken("t-2", 20L, now.plusHours(2), "tenantX"));

    var allTenantX = repository.findByUserId(20L);
    assertThat(allTenantX).hasSize(2);

    var unusedDesc = repository.findUnusedTokensByUserId(20L);
    assertThat(unusedDesc.get(0).getToken()).isEqualTo("t-2");
  }

  @Test
  @DisplayName("deleteByExpiresAtBefore and countByExpiresAtBefore should remove expired and count correctly")
  void deleteExpired() {
    var now = LocalDateTime.now();
    repository.saveAll(List.of(
        new VerificationToken("e1", 30L, now.minusHours(2), "tZ"),
        new VerificationToken("e2", 30L, now.minusMinutes(1), "tZ"),
        new VerificationToken("ok", 30L, now.plusHours(1), "tZ")
    ));

    var countBefore = repository.countByExpiresAtBefore(now);
    assertThat(countBefore).isEqualTo(2);

    var deleted = repository.deleteByExpiresAtBefore(now);
    assertThat(deleted).isEqualTo(2);

    TenantContext.setCurrentTenantId("tZ");
    var remaining = repository.findByUserId(30L);
    assertThat(remaining).extracting(VerificationToken::getToken).containsExactly("ok");
  }

  @Test
  @DisplayName("markAllUnusedTokensAsUsedByUserIdAndTenantId should invalidate previous tokens")
  void invalidateUnused() {
    var now = LocalDateTime.now();
    TenantContext.setCurrentTenantId("t1");
    repository.save(new VerificationToken("iv1", 40L, now.plusHours(2), "t1"));
    repository.save(new VerificationToken("iv2", 40L, now.plusHours(2), "t1"));

    var updated = repository.markAllUnusedTokensAsUsedByUserId(40L);
    assertThat(updated).isEqualTo(2);

    var unused = repository.findUnusedTokensByUserId(40L);
    assertThat(unused).isEmpty();
  }

  @Test
  @DisplayName("countTokensCreatedSince and findExpiredTokens should reflect time windows in schema")
  void createdSinceAndExpiredByTenant() {
    var now = LocalDateTime.now();
    var ten = "TEN";
    repository.saveAll(List.of(
        new VerificationToken("c1", 50L, now.plusHours(1), ten),
        new VerificationToken("c2", 50L, now.plusHours(1), ten)
    ));

    var since = now.minusMinutes(1);
    TenantContext.setCurrentTenantId(ten);
    var countSince = repository.countTokensCreatedSince(50L, since);
    assertThat(countSince).isEqualTo(2);

    repository.saveAll(List.of(
        new VerificationToken("ex1", 51L, now.minusHours(3), ten),
        new VerificationToken("ex2", 52L, now.minusHours(2), ten)
    ));

    var expired = repository.findExpiredTokens(now);
    assertThat(expired).hasSize(2);
  }

  @TestConfiguration
  static class MultiTenantTestConfig {
    @Bean
    SchemaNameResolver schemaNameResolver() {
      return new SchemaNameResolver("tenant_");
    }

    @Bean
    SchemaTenantIdentifierResolver schemaTenantIdentifierResolver(SchemaNameResolver resolver) {
      return new SchemaTenantIdentifierResolver(resolver);
    }
  }
}
