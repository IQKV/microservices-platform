-- Contact Service Database Initialization
-- This script initializes the database for the IQ Key Value Contact Service

-- Create database if it doesn't exist (handled by POSTGRES_DB env var)
-- CREATE DATABASE IF NOT EXISTS foundation_contact;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Create schema for contact service
CREATE SCHEMA IF NOT EXISTS contact;

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA contact TO foundation_contact;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA contact TO foundation_contact;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA contact TO foundation_contact;

-- Set default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA contact GRANT ALL ON TABLES TO foundation_contact;
ALTER DEFAULT PRIVILEGES IN SCHEMA contact GRANT ALL ON SEQUENCES TO foundation_contact;

-- Create initial tables (these will be managed by Flyway/Liquibase in the application)
-- This is just for basic setup

COMMENT ON SCHEMA contact IS 'Schema for IQ Key Value Contact Service';