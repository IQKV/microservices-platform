-- Pipeline Service Database Initialization
-- This script initializes the database for the IQ Key Value Pipeline Service

-- Create database if it doesn't exist (handled by POSTGRES_DB env var)
-- CREATE DATABASE IF NOT EXISTS foundation_pipeline;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Create schema for pipeline service
CREATE SCHEMA IF NOT EXISTS pipeline;

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA pipeline TO foundation_pipeline;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA pipeline TO foundation_pipeline;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA pipeline TO foundation_pipeline;

-- Set default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA pipeline GRANT ALL ON TABLES TO foundation_pipeline;
ALTER DEFAULT PRIVILEGES IN SCHEMA pipeline GRANT ALL ON SEQUENCES TO foundation_pipeline;

-- Create initial tables (these will be managed by Flyway/Liquibase in the application)
-- This is just for basic setup

COMMENT ON SCHEMA pipeline IS 'Schema for IQ Key Value Pipeline Service';