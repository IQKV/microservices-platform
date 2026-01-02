package com.iqscaffold.billingservice.tenancy;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TenantLiquibaseRunner {

  private final DataSource dataSource;
  private final String tenantChangeLog;
  private final String systemChangeLog;

  public TenantLiquibaseRunner(
      final DataSource dataSource,
      @Value("${iqscaffold.liquibase.tenantChangeLog:classpath:db/changelog/tenant/master.xml}") final String tenantChangeLog,
      @Value("${iqscaffold.liquibase.systemChangeLog:classpath:db/changelog/system/master.xml}") final String systemChangeLog) {
    this.dataSource = dataSource;
    this.tenantChangeLog = tenantChangeLog;
    this.systemChangeLog = systemChangeLog;
  }

  // System changelog typically sets up shared tables or the public schema itself if needed
  public void runSystemChangelog() throws Exception {
    var liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setDefaultSchema("public");
    liquibase.setLiquibaseSchema("public");
    liquibase.setChangeLog(systemChangeLog);
    liquibase.afterPropertiesSet();
  }

  // Runs migrations for a specific tenant schema
  public void runTenantChangelog(String schema) throws Exception {
    var liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setDefaultSchema(schema);
    liquibase.setLiquibaseSchema(schema);
    liquibase.setChangeLog(tenantChangeLog);
    liquibase.afterPropertiesSet();
  }
}
