-- Initialize Billing Service Database
-- This script runs when the PostgreSQL container starts for the first time

-- Create additional schemas if needed
CREATE SCHEMA IF NOT EXISTS billing;

-- Set default search path
ALTER DATABASE iqscaffold_billing SET search_path TO public, billing;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE iqscaffold_billing TO iqscaffold_billing;
GRANT ALL PRIVILEGES ON SCHEMA public TO iqscaffold_billing;
GRANT ALL PRIVILEGES ON SCHEMA billing TO iqscaffold_billing;

-- Create audit function for tracking changes
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Log initialization
INSERT INTO pg_stat_statements_reset();
SELECT 'Billing Service Database initialized successfully' AS status;
