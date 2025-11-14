-- Initialize User Service Schema
-- This script creates the basic schema structure for the auth service
-- Note: Liquibase will handle the actual table creation

-- Set the search path
SET
search_path TO auth, public;

-- Create sequences for manual ID generation if needed
CREATE SEQUENCE IF NOT EXISTS auth.hibernate_sequence START 1000;

-- Create indexes for performance (these will be recreated by Liquibase if needed)
-- These are just placeholders - actual indexes are managed by Liquibase migrations

-- Grant permissions to application user
GRANT
USAGE,
SELECT
ON ALL SEQUENCES IN SCHEMA auth TO auth_app_user;
GRANT
SELECT,
INSERT
,
UPDATE,
DELETE
ON ALL TABLES IN SCHEMA auth TO auth_app_user;

-- Set default privileges for future objects
ALTER
DEFAULT PRIVILEGES IN SCHEMA auth GRANT
SELECT,
INSERT
,
UPDATE,
DELETE
ON TABLES TO auth_app_user;
ALTER
DEFAULT PRIVILEGES IN SCHEMA auth GRANT USAGE,
SELECT
ON SEQUENCES TO auth_app_user;