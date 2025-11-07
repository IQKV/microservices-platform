import { FullConfig } from "@playwright/test";
import { testEnvironment } from "./playwright.config";

/**
 * Global teardown for Playwright tests
 * Runs once after all tests complete
 */
async function globalTeardown(config: FullConfig) {
  console.log(`🧹 Starting global teardown for ${testEnvironment.name} environment`);

  // Perform environment-specific cleanup
  if (testEnvironment.isLocal) {
    await cleanupLocalEnvironment();
  }

  // Generate test summary
  generateTestSummary();

  console.log("✅ Global teardown completed successfully");
}

/**
 * Cleanup local test environment
 */
async function cleanupLocalEnvironment() {
  console.log("🧹 Cleaning up local test environment...");

  // Note: Actual database cleanup will be handled by individual tests
  // This is just for logging and final cleanup tasks

  console.log("💡 Remember to clean up test containers with: npm run test:docker:down");
}

/**
 * Generate test execution summary
 */
function generateTestSummary() {
  const timestamp = new Date().toISOString();
  const summary = {
    environment: testEnvironment.name,
    timestamp,
    baseURL: testEnvironment.config.baseURL,
    services: testEnvironment.config.services,
  };

  console.log("📊 Test Execution Summary:");
  console.log(JSON.stringify(summary, null, 2));
}

export default globalTeardown;
