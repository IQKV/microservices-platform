-- Initialize User Service Database
-- This script runs when the PostgreSQL container starts for the first time

-- Create additional schemas if needed
CREATE SCHEMA IF NOT EXISTS auth;

-- Set default search path
ALTER DATABASE foundation_user SET search_path TO public, auth;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE foundation_user TO foundation_user;
GRANT ALL PRIVILEGES ON SCHEMA public TO foundation_user;
GRANT ALL PRIVILEGES ON SCHEMA auth TO foundation_user;

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
SELECT 'User Service Database initialized successfully' AS status;