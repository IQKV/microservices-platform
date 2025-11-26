-- Initialize bookstore database
-- This script is executed when the PostgreSQL container starts

-- Create database if it doesn't exist (handled by POSTGRES_DB env var)
-- Create user if it doesn't exist (handled by POSTGRES_USER env var)

-- Grant necessary permissions
GRANT
ALL
PRIVILEGES
ON
DATABASE
bookstore_db TO bookstore_user;

-- Create schema if needed
CREATE SCHEMA IF NOT EXISTS bookstore AUTHORIZATION bookstore_user;

-- Set default schema
ALTER
USER bookstore_user SET search_path TO bookstore, public;

-- Create extensions if needed
CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE
EXTENSION IF NOT EXISTS "pg_trgm";

-- Grant usage on schema
GRANT USAGE ON SCHEMA
bookstore TO bookstore_user;
GRANT CREATE
ON SCHEMA bookstore TO bookstore_user;

-- Set connection limits (optional)
ALTER
USER bookstore_user CONNECTION LIMIT 50;

-- Configure default privileges for future objects
ALTER
DEFAULT PRIVILEGES IN SCHEMA bookstore GRANT ALL ON TABLES TO bookstore_user;
ALTER
DEFAULT PRIVILEGES IN SCHEMA bookstore GRANT ALL ON SEQUENCES TO bookstore_user;
ALTER
DEFAULT PRIVILEGES IN SCHEMA bookstore GRANT ALL ON FUNCTIONS TO bookstore_user;