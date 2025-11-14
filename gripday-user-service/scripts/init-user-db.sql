-- Initialize User Service Database
-- This script sets up the basic database configuration for the auth service

-- Create extensions if they don't exist
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE
EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Create application schema
CREATE SCHEMA IF NOT EXISTS auth;

-- Set default search path
ALTER
DATABASE gripday_user SET search_path TO auth, public;

-- Create application user with limited privileges (if not exists)
DO
$$
BEGIN
    IF
NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'auth_app_user') THEN
CREATE ROLE auth_app_user WITH LOGIN PASSWORD 'auth_app_password';
END IF;
END
$$;

-- Grant necessary permissions
GRANT CONNECT
ON DATABASE gripday_user TO auth_app_user;
GRANT USAGE ON SCHEMA
auth TO auth_app_user;
GRANT CREATE
ON SCHEMA auth TO auth_app_user;

-- Set up basic configuration
ALTER
SYSTEM SET log_statement = 'mod';
ALTER
SYSTEM SET log_min_duration_statement = 1000;
ALTER
SYSTEM SET shared_preload_libraries = 'pg_stat_statements';

-- Reload configuration
SELECT pg_reload_conf();