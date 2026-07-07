-- Initialize Billing Service Database
-- This script runs when the PostgreSQL container starts for the first time

-- Create additional schemas
CREATE SCHEMA IF NOT EXISTS billingservice;

CREATE SCHEMA IF NOT EXISTS t_platform;
CREATE SCHEMA IF NOT EXISTS t_demo0001;
CREATE SCHEMA IF NOT EXISTS t_acme0001;

-- Set default search path
ALTER DATABASE billingservice SET search_path TO t_platform, public, billingservice;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE billingservice TO svc_billing_dba;
GRANT ALL PRIVILEGES ON SCHEMA public TO svc_billing_dba;
GRANT ALL PRIVILEGES ON SCHEMA billingservice TO svc_billing_dba;
GRANT ALL PRIVILEGES ON SCHEMA t_platform TO svc_billing_dba;
GRANT ALL PRIVILEGES ON SCHEMA t_demo0001 TO svc_billing_dba;
GRANT ALL PRIVILEGES ON SCHEMA t_acme0001 TO svc_billing_dba;

-- Audit trigger function for tracking row updates
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

SELECT 'Billing Service Database initialized successfully' AS status;
