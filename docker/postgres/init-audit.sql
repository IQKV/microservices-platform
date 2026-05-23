-- Initialize Audit Service Database
-- This script runs when the PostgreSQL container starts for the first time

-- Create additional schemas
CREATE SCHEMA IF NOT EXISTS audit;

-- Set default search path
ALTER DATABASE audit SET search_path TO public, audit;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE audit TO svc_audit_dba;
GRANT ALL PRIVILEGES ON SCHEMA public TO svc_audit_dba;
GRANT ALL PRIVILEGES ON SCHEMA audit TO svc_audit_dba;

-- Audit trigger function for tracking row updates
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

SELECT 'Audit Service Database initialized successfully' AS status;
