-- Initialize Auth Service Database
-- This script runs when the PostgreSQL container starts for the first time

-- Create additional schemas if needed
CREATE SCHEMA IF NOT EXISTS auth;

-- Set default search path
ALTER DATABASE gripday_auth SET search_path TO public, auth;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE gripday_auth TO gripday_user;
GRANT ALL PRIVILEGES ON SCHEMA public TO gripday_user;
GRANT ALL PRIVILEGES ON SCHEMA auth TO gripday_user;

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
SELECT 'Auth Service Database initialized successfully' AS status;