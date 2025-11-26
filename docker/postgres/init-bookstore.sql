-- Initialize Bookstore Database
-- This script sets up the initial database structure for the bookstore service

-- Create database if it doesn't exist (handled by Docker environment variables)
-- The database 'iqscaffold_bookstore' is created automatically by the PostgreSQL container

-- Grant necessary permissions to the user
GRANT ALL PRIVILEGES ON DATABASE iqscaffold_bookstore TO iqscaffold_user;

-- Connect to the bookstore database
\c iqscaffold_bookstore;

-- Create schema for bookstore service
CREATE SCHEMA IF NOT EXISTS bookstore;

-- Grant schema permissions
GRANT ALL ON SCHEMA bookstore TO iqscaffold_user;
GRANT ALL ON SCHEMA public TO iqscaffold_user;

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO iqscaffold_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO iqscaffold_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA bookstore GRANT ALL ON TABLES TO iqscaffold_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA bookstore GRANT ALL ON SEQUENCES TO iqscaffold_user;

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Note: Actual table creation is handled by Liquibase migrations
-- This script only sets up the database and permissions