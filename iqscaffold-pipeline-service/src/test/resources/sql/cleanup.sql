-- Cleanup script for integration tests
-- This script is executed after each test method to ensure clean state

DELETE FROM pipeline_activity;
DELETE FROM follow_up;
DELETE FROM pipeline_item;
DELETE FROM pipeline_stage;

-- Reset sequences if using H2 database
ALTER SEQUENCE IF EXISTS pipeline_stage_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS pipeline_item_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS follow_up_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS pipeline_activity_seq RESTART WITH 1;