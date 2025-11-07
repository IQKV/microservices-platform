import { defineConfig, devices } from "@playwright/test";
import * as dotenv from "dotenv";

// Load environment variables
dotenv.config();

// Determine test environment
const testEnv = process.env.TEST_ENV || "local";

// Environment-specific configurations
const environments = {
  local: {
    baseURL: "http://localhost:8080",
    services: {
      gateway: "http://localhost:8080",
      auth: "http://localhost:8081",
      bookstore: "http://localhost:8082",
    },
    timeout: 30000,
    retries: 1,
    workers: 4,
  },
  staging: {
    baseURL: "https://api.pynity.website",
    services: {
      gateway: "https://api.pynity.website",
      auth: "https://staging-auth.pynity.com",
      bookstore: "https://staging-bookstore.pynity.com",
    },
    timeout: 60000,
    retries: 2,
    workers: 2,
  },
  production: {
    baseURL: "https://api.pynity.com",
    services: {
      gateway: "https://api.pynity.com",
      auth: "https://auth.pynity.com",
      bookstore: "https://bookstore.pynity.com",
    },
    timeout: 90000,
    retries: 3,
    workers: 1,
  },
};

const config = environments[testEnv as keyof typeof environments] || environments.local;

/**
 * Playwright configuration for Gripday API testing
 * @see https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
  testDir: "./src/tests",

  /* Global test timeout */
  timeout: config.timeout,

  /* Test execution settings */
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? config.retries : 1,
  workers: process.env.CI ? config.workers : config.workers,

  /* Reporter configuration */
  reporter: [
    [
      "html",
      {
        outputFolder: "reports/html",
        open: process.env.CI ? "never" : "on-failure",
      },
    ],
    [
      "junit",
      {
        outputFile: "reports/junit.xml",
      },
    ],
    [
      "json",
      {
        outputFile: "reports/results.json",
      },
    ],
    ["line"],
  ],

  /* Global test setup and teardown */
  globalSetup: "./global-setup.ts",
  globalTeardown: "./global-teardown.ts",

  /* Shared settings for all tests */
  use: {
    /* Base URL for API requests */
    baseURL: config.baseURL,

    /* Collect trace on retry */
    trace: "retain-on-failure",

    /* Screenshot on failure */
    screenshot: "only-on-failure",

    /* Video recording */
    video: "retain-on-failure",

    /* Extra HTTP headers */
    extraHTTPHeaders: {
      Accept: "application/json",
      "Content-Type": "application/json",
      "User-Agent": "Gripday-E2E-Tests/1.0.0",
    },

    /* API request timeout */
    actionTimeout: 15000,

    /* Navigation timeout */
    navigationTimeout: 30000,

    /* Ignore HTTPS errors in non-production environments */
    ignoreHTTPSErrors: testEnv !== "production",
  },

  /* Configure projects for different test types */
  projects: [
    {
      name: "api-tests",
      testMatch: /.*\.spec\.ts$/,
      use: {
        ...devices["Desktop Chrome"],
        // API tests don't need browser context
        headless: true,
      },
    },

    {
      name: "auth-service",
      testDir: "./src/tests/auth-service",
      use: {
        ...devices["Desktop Chrome"],
        headless: true,
      },
    },

    {
      name: "gateway-service",
      testDir: "./src/tests/gateway-service",
      use: {
        ...devices["Desktop Chrome"],
        headless: true,
      },
    },

    {
      name: "bookstore-service",
      testDir: "./src/tests/bookstore-service",
      use: {
        ...devices["Desktop Chrome"],
        headless: true,
      },
    },

    {
      name: "integration",
      testDir: "./src/tests/integration",
      use: {
        ...devices["Desktop Chrome"],
        headless: true,
      },
    },

    {
      name: "security",
      testDir: "./src/tests/security",
      use: {
        ...devices["Desktop Chrome"],
        headless: true,
      },
    },
  ],

  /* Output directories */
  outputDir: "test-results/",

  /* Environment-specific test configuration */
  metadata: {
    environment: testEnv,
    baseURL: config.baseURL,
    services: config.services,
    timestamp: new Date().toISOString(),
  },
});

/* Export environment configuration for use in tests */
export const testEnvironment = {
  name: testEnv,
  config: config,
  isLocal: testEnv === "local",
  isStaging: testEnv === "staging",
  isProduction: testEnv === "production",
};
