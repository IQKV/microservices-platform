-- Lead Service Database Initialization
-- This script initializes the database for the IQ Scaffold Lead Service

-- Create database if it doesn't exist (handled by POSTGRES_DB env var)
-- CREATE DATABASE IF NOT EXISTS iqscaffold_lead;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Create schema for lead service
CREATE SCHEMA IF NOT EXISTS lead;

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA lead TO iqscaffold_lead;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA lead TO iqscaffold_lead;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA lead TO iqscaffold_lead;

-- Set default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA lead GRANT ALL ON TABLES TO iqscaffold_lead;
ALTER DEFAULT PRIVILEGES IN SCHEMA lead GRANT ALL ON SEQUENCES TO iqscaffold_lead;

-- Create initial tables (these will be managed by Flyway/Liquibase in the application)
-- This is just for basic setup

COMMENT ON SCHEMA lead IS 'Schema for IQ Scaffold Lead Service';