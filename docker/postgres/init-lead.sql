-- Lead Service Database Initialization
-- This script initializes the database for the IQ Key Value Lead Service

-- Create database if it doesn't exist (handled by POSTGRES_DB env var)
-- CREATE DATABASE IF NOT EXISTS foundation_lead;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Create schema for lead service
CREATE SCHEMA IF NOT EXISTS lead;

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA lead TO foundation_lead;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA lead TO foundation_lead;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA lead TO foundation_lead;

-- Set default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA lead GRANT ALL ON TABLES TO foundation_lead;
ALTER DEFAULT PRIVILEGES IN SCHEMA lead GRANT ALL ON SEQUENCES TO foundation_lead;

-- Create initial tables (these will be managed by Flyway/Liquibase in the application)
-- This is just for basic setup

COMMENT ON SCHEMA lead IS 'Schema for IQ Key Value Lead Service';