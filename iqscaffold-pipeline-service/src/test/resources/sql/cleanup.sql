-- Cleanup script for integration tests
-- This script is executed after each test method to ensure clean state

DELETE FROM pipeline_activities;
DELETE FROM follow_ups;
DELETE FROM pipeline_items;
DELETE FROM pipeline_stages;

-- Reset sequences if using H2 database
ALTER SEQUENCE IF EXISTS pipeline_stages_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS pipeline_items_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS follow_ups_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS pipeline_activities_seq RESTART WITH 1;