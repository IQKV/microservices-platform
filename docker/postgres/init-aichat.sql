-- Initialize AI Chat Service Database
-- This script runs when the PostgreSQL container starts for the first time.
-- Chat sessions and messages are global today, so the tenant schemas below are
-- pre-created for layout parity with the other service databases.

-- Create additional schemas
CREATE SCHEMA IF NOT EXISTS aichat;

CREATE SCHEMA IF NOT EXISTS t_platform;
CREATE SCHEMA IF NOT EXISTS t_demo0001;
CREATE SCHEMA IF NOT EXISTS t_acme0001;

-- Set default search path
ALTER DATABASE aichatservice SET search_path TO t_platform, public, aichat;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE aichatservice TO svc_aichat_dba;
GRANT ALL PRIVILEGES ON SCHEMA public TO svc_aichat_dba;
GRANT ALL PRIVILEGES ON SCHEMA aichat TO svc_aichat_dba;
GRANT ALL PRIVILEGES ON SCHEMA t_platform TO svc_aichat_dba;
GRANT ALL PRIVILEGES ON SCHEMA t_demo0001 TO svc_aichat_dba;
GRANT ALL PRIVILEGES ON SCHEMA t_acme0001 TO svc_aichat_dba;

-- Audit trigger function for tracking row updates
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

SELECT 'AI Chat Service Database initialized successfully' AS status;
