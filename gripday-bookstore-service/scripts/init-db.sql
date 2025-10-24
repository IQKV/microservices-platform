-- Initialize bookstore database
-- This script is executed when the PostgreSQL container starts

-- Create database if it doesn't exist (handled by POSTGRES_DB env var)
-- Create user if it doesn't exist (handled by POSTGRES_USER env var)

-- Grant necessary permissions
GRANT ALL PRIVILEGES ON DATABASE bookstore_db TO bookstore_user;

-- Create schema if needed
CREATE SCHEMA IF NOT EXISTS bookstore AUTHORIZATION bookstore_user;

-- Set default schema
ALTER USER bookstore_user SET search_path TO bookstore, public;