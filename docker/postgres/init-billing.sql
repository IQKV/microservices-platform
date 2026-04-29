-- Initialize Billing Service Database
-- This script runs when the PostgreSQL container starts for the first time

-- Create additional schemas
CREATE SCHEMA IF NOT EXISTS billing;

-- Set default search path
ALTER DATABASE billing SET search_path TO public, billing;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE billing TO svc_billing_dba;
GRANT ALL PRIVILEGES ON SCHEMA public TO svc_billing_dba;
GRANT ALL PRIVILEGES ON SCHEMA billing TO svc_billing_dba;

-- Audit trigger function for tracking row updates
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

SELECT 'Billing Service Database initialized successfully' AS status;
