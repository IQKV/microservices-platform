-- Initialize IAM Service Database
-- This script runs when the PostgreSQL container starts for the first time

-- Create additional schemas
CREATE SCHEMA IF NOT EXISTS auth;

-- Set default search path
ALTER DATABASE iam SET search_path TO public, auth;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE iam TO svc_iam_dba;
GRANT ALL PRIVILEGES ON SCHEMA public TO svc_iam_dba;
GRANT ALL PRIVILEGES ON SCHEMA auth TO svc_iam_dba;

-- Audit trigger function for tracking row updates
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

SELECT 'IAM Service Database initialized successfully' AS status;
