-- Contact Service Database Initialization
-- This script initializes the database for the IQ Scaffold Contact Service

-- Create database if it doesn't exist (handled by POSTGRES_DB env var)
-- CREATE DATABASE IF NOT EXISTS iqscaffold_contact;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Create schema for contact service
CREATE SCHEMA IF NOT EXISTS contact;

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA contact TO iqscaffold_contact;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA contact TO iqscaffold_contact;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA contact TO iqscaffold_contact;

-- Set default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA contact GRANT ALL ON TABLES TO iqscaffold_contact;
ALTER DEFAULT PRIVILEGES IN SCHEMA contact GRANT ALL ON SEQUENCES TO iqscaffold_contact;

-- Create initial tables (these will be managed by Flyway/Liquibase in the application)
-- This is just for basic setup

COMMENT ON SCHEMA contact IS 'Schema for IQ Scaffold Contact Service';