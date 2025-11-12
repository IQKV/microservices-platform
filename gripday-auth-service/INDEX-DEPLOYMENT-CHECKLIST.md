# Index Optimization Deployment Checklist

**Migration**: 009-add-missing-critical-indexes.xml  
**Date**: November 12, 2024  
**Indexes**: 8 new composite indexes

---

## Pre-Deployment

### Documentation Review

- [ ] Read `INDEX-OPTIMIZATION-REPORT.md`
- [ ] Review `docs/INDEXES-SUMMARY.md`
- [ ] Understand `docs/missing-indexes-analysis.md`
- [ ] Read `docs/index-implementation-guide.md`

### Technical Preparation

- [ ] Verify Liquibase migration file exists: `009-add-missing-critical-indexes.xml`
- [ ] Confirm master changelog updated: `db.changelog-master.xml`
- [ ] Check database disk space (need ~5-10% additional space)
- [ ] Verify database backup strategy in place

---

## Local Environment Testing

### Setup

- [ ] Start local PostgreSQL: `docker-compose up -d postgres`
- [ ] Verify database connectivity
- [ ] Check current database state

### Deployment

- [ ] Run application: `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
- [ ] Monitor logs for migration success
- [ ] Verify no errors in application startup

### Verification

- [ ] Connect to database: `psql -h localhost -U gripday_auth_user -d gripday_auth_db`
- [ ] List all new indexes: `\di+ idx_*`
- [ ] Verify 8 new indexes created:
  - [ ] `idx_users_tenant_email_verified`
  - [ ] `idx_email_verification_user_used`
  - [ ] `idx_email_verification_token_used`
  - [ ] `idx_email_verification_tenant_user_used`
  - [ ] `idx_email_verification_user_created`
  - [ ] `idx_user_audit_log_user_tenant_created`
  - [ ] `idx_user_audit_log_login_failures`
  - [ ] `idx_organizations_tenant_enabled`

### Performance Testing

- [ ] Run token validation query with EXPLAIN ANALYZE
- [ ] Run failed login query with EXPLAIN ANALYZE
- [ ] Run unverified users query with EXPLAIN ANALYZE
- [ ] Verify indexes are being used in query plans
- [ ] Document baseline performance metrics

---

## Staging Environment Deployment

### Pre-Deployment

- [ ] Schedule deployment window
- [ ] Notify team of deployment
- [ ] Backup staging database
- [ ] Document current performance metrics

### Deployment

- [ ] Set staging profile: `export SPRING_PROFILES_ACTIVE=staging`
- [ ] Deploy application
- [ ] Monitor application logs
- [ ] Verify migration completed successfully

### Verification

- [ ] Connect to staging database
- [ ] Verify all 8 indexes created
- [ ] Check index sizes: `SELECT indexrelname, pg_size_pretty(pg_relation_size(indexrelid)) FROM pg_stat_user_indexes WHERE indexrelname LIKE 'idx_%';`
- [ ] Run EXPLAIN ANALYZE on critical queries
- [ ] Verify query plans use new indexes

### Performance Validation

- [ ] Test authentication endpoints
- [ ] Test email verification endpoints
- [ ] Test audit log endpoints
- [ ] Test user management endpoints
- [ ] Compare performance with baseline metrics
- [ ] Document performance improvements

### Monitoring (24-48 hours)

- [ ] Monitor application response times
- [ ] Check database CPU and I/O metrics
- [ ] Review slow query logs
- [ ] Monitor index usage statistics
- [ ] Check for any errors or warnings

---

## Production Environment Deployment

### Pre-Deployment Planning

- [ ] Schedule maintenance window (if needed)
- [ ] Notify stakeholders of deployment
- [ ] Prepare rollback plan
- [ ] Review staging deployment results
- [ ] Confirm team availability for monitoring

### Pre-Deployment Backup

- [ ] Full database backup: `pg_dump -h production-db -U gripday_auth_user gripday_auth_db > backup_$(date +%Y%m%d_%H%M%S).sql`
- [ ] Verify backup integrity
- [ ] Store backup in secure location
- [ ] Document backup location and timestamp

### Deployment

- [ ] Deploy application to production
- [ ] Monitor application startup logs
- [ ] Verify migration completed successfully
- [ ] Check for any errors or warnings

### Immediate Verification (Within 5 minutes)

- [ ] Connect to production database
- [ ] Verify all 8 indexes created successfully
- [ ] Check index creation completed without errors
- [ ] Verify application is responding normally
- [ ] Test critical endpoints (login, verification)

### Performance Validation (Within 30 minutes)

- [ ] Run EXPLAIN ANALYZE on critical queries
- [ ] Verify indexes are being used
- [ ] Check query execution times
- [ ] Compare with baseline metrics
- [ ] Document performance improvements

### Monitoring (First 24 hours)

- [ ] Monitor application response times
- [ ] Check database metrics (CPU, I/O, connections)
- [ ] Review application logs for errors
- [ ] Monitor index usage statistics
- [ ] Check slow query logs
- [ ] Verify no performance degradation

### Extended Monitoring (First Week)

- [ ] Daily review of performance metrics
- [ ] Monitor index usage patterns
- [ ] Check for any anomalies
- [ ] Gather user feedback
- [ ] Document any issues or observations

---

## Post-Deployment Validation

### Database Health Checks

- [ ] Index usage statistics: `SELECT * FROM pg_stat_user_indexes WHERE indexrelname LIKE 'idx_%' ORDER BY idx_scan DESC;`
- [ ] Index hit ratio: `SELECT sum(idx_blks_hit) / nullif(sum(idx_blks_hit + idx_blks_read), 0) AS index_hit_ratio FROM pg_statio_user_indexes;`
- [ ] Table scan statistics: `SELECT * FROM pg_stat_user_tables WHERE schemaname = 'public' ORDER BY seq_scan DESC;`
- [ ] Index bloat check (if available)

### Performance Metrics

- [ ] Authentication response time: **\_\_\_** ms (Target: <50ms)
- [ ] Token validation time: **\_\_\_** ms (Target: <10ms)
- [ ] Failed login check time: **\_\_\_** ms (Target: <10ms)
- [ ] Unverified users query: **\_\_\_** ms (Target: <30ms)
- [ ] User audit logs query: **\_\_\_** ms (Target: <35ms)
- [ ] Organization queries: **\_\_\_** ms (Target: <33ms)

### Success Criteria

- [ ] All 8 indexes created successfully
- [ ] No application errors or warnings
- [ ] Query plans show index usage
- [ ] Performance improvements meet expectations
- [ ] No user-reported issues
- [ ] Database metrics within normal ranges

---

## Rollback Procedure (If Needed)

### Automatic Rollback

- [ ] Stop application
- [ ] Run: `./mvnw liquibase:rollback -Dliquibase.rollbackCount=1`
- [ ] Verify indexes removed
- [ ] Restart application
- [ ] Verify application functionality

### Manual Rollback

- [ ] Connect to database
- [ ] Execute DROP INDEX statements (see INDEX-OPTIMIZATION-REPORT.md)
- [ ] Verify indexes removed
- [ ] Restart application
- [ ] Verify application functionality

### Post-Rollback

- [ ] Document reason for rollback
- [ ] Analyze root cause
- [ ] Update deployment plan
- [ ] Schedule re-deployment (if applicable)

---

## Documentation & Reporting

### Deployment Documentation

- [ ] Document deployment date and time
- [ ] Record any issues encountered
- [ ] Document performance improvements
- [ ] Update deployment history

### Performance Report

- [ ] Baseline metrics (before deployment)
- [ ] Post-deployment metrics
- [ ] Performance improvement percentages
- [ ] Query execution plan comparisons

### Lessons Learned

- [ ] What went well
- [ ] What could be improved
- [ ] Recommendations for future deployments

---

## Sign-off

### Local Environment

- [ ] Tested by: ********\_\_\_******** Date: ****\_\_\_****
- [ ] Verified by: ********\_\_\_******** Date: ****\_\_\_****

### Staging Environment

- [ ] Deployed by: ********\_\_\_******** Date: ****\_\_\_****
- [ ] Verified by: ********\_\_\_******** Date: ****\_\_\_****
- [ ] Performance validated by: ********\_\_\_******** Date: ****\_\_\_****

### Production Environment

- [ ] Approved by: ********\_\_\_******** Date: ****\_\_\_****
- [ ] Deployed by: ********\_\_\_******** Date: ****\_\_\_****
- [ ] Verified by: ********\_\_\_******** Date: ****\_\_\_****
- [ ] Sign-off by: ********\_\_\_******** Date: ****\_\_\_****

---

## Quick Reference

### Key Files

- Migration: `src/main/resources/db/changelog/009-add-missing-critical-indexes.xml`
- Report: `INDEX-OPTIMIZATION-REPORT.md`
- Summary: `docs/INDEXES-SUMMARY.md`
- Guide: `docs/index-implementation-guide.md`

### Key Commands

```bash
# Start local database
docker-compose up -d postgres

# Run application
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Connect to database
psql -h localhost -U gripday_auth_user -d gripday_auth_db

# List indexes
\di+ idx_*

# Rollback
./mvnw liquibase:rollback -Dliquibase.rollbackCount=1
```

### Support Contacts

- Platform Team: ********\_\_\_********
- Database Admin: ********\_\_\_********
- On-Call Engineer: ********\_\_\_********

---

**Checklist Version**: 1.0  
**Last Updated**: November 12, 2024  
**Status**: Ready for Use
