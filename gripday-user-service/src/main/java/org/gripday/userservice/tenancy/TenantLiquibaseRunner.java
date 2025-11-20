package org.gripday.userservice.tenancy;

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
      @Value("${app.liquibase.tenantChangeLog:classpath:db/changelog/tenant/master.xml}") final String tenantChangeLog,
      @Value("${app.liquibase.systemChangeLog:classpath:db/changelog/system/master.xml}") final String systemChangeLog) {
    this.dataSource = dataSource;
    this.tenantChangeLog = tenantChangeLog;
    this.systemChangeLog = systemChangeLog;
  }

  public void runSystemChangelog() throws Exception {
    var liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setDefaultSchema("public");
    liquibase.setLiquibaseSchema("public");
    liquibase.setChangeLog(systemChangeLog);
    liquibase.afterPropertiesSet();
  }

  public void runTenantChangelog(String schema) throws Exception {
    var liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setDefaultSchema(schema);
    liquibase.setLiquibaseSchema(schema);
    liquibase.setChangeLog(tenantChangeLog);
    liquibase.afterPropertiesSet();
  }
}