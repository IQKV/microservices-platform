-- Initialize Audit Service Database
-- This script runs when the PostgreSQL container starts for the first time

-- Create additional schemas
CREATE SCHEMA IF NOT EXISTS auditservice;

CREATE SCHEMA IF NOT EXISTS t_platform;
CREATE SCHEMA IF NOT EXISTS t_demo0001;
CREATE SCHEMA IF NOT EXISTS t_acme0001;

-- Set default search path
ALTER DATABASE auditservice SET search_path TO t_platform, public, auditservice;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE auditservice TO svc_audit_dba;
GRANT ALL PRIVILEGES ON SCHEMA public TO svc_audit_dba;
GRANT ALL PRIVILEGES ON SCHEMA auditservice TO svc_audit_dba;
GRANT ALL PRIVILEGES ON SCHEMA t_platform TO svc_audit_dba;
GRANT ALL PRIVILEGES ON SCHEMA t_demo0001 TO svc_audit_dba;
GRANT ALL PRIVILEGES ON SCHEMA t_acme0001 TO svc_audit_dba;

-- Audit trigger function for tracking row updates
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

SELECT 'Audit Service Database initialized successfully' AS status;
