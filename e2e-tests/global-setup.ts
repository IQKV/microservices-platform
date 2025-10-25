import { chromium, FullConfig } from '@playwright/test';
import { testEnvironment } from './playwright.config';

/**
 * Global setup for Playwright tests
 * Runs once before all tests
 */
async function globalSetup(config: FullConfig) {
  console.log(`🚀 Starting global setup for ${testEnvironment.name} environment`);
  
  // Validate environment health before running tests
  if (testEnvironment.isLocal) {
    await validateLocalEnvironment();
  } else {
    await validateRemoteEnvironment();
  }
  
  console.log('✅ Global setup completed successfully');
}

/**
 * Validate local test environment health
 */
async function validateLocalEnvironment() {
  console.log('🔍 Validating local test environment...');
  
  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();
  
  try {
    // Check if services are running
    const services = testEnvironment.config.services;
    
    for (const [serviceName, serviceUrl] of Object.entries(services)) {
      console.log(`Checking ${serviceName} service at ${serviceUrl}...`);
      
      try {
        const response = await page.request.get(`${serviceUrl}/actuator/health`, {
          timeout: 10000
        });
        
        if (response.ok()) {
          console.log(`✅ ${serviceName} service is healthy`);
        } else {
          console.warn(`⚠️  ${serviceName} service returned status ${response.status()}`);
        }
      } catch (error) {
        console.warn(`⚠️  ${serviceName} service is not accessible: ${error}`);
        console.log(`💡 Make sure to start the service with: docker-compose up ${serviceName}`);
      }
    }
  } finally {
    await context.close();
    await browser.close();
  }
}

/**
 * Validate remote test environment health
 */
async function validateRemoteEnvironment() {
  console.log(`🔍 Validating ${testEnvironment.name} environment...`);
  
  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();
  
  try {
    // Check if gateway is accessible
    const gatewayUrl = testEnvironment.config.services.gateway;
    console.log(`Checking gateway at ${gatewayUrl}...`);
    
    const response = await page.request.get(`${gatewayUrl}/actuator/health`, {
      timeout: 15000
    });
    
    if (response.ok()) {
      console.log('✅ Gateway service is healthy');
    } else {
      throw new Error(`Gateway service returned status ${response.status()}`);
    }
  } finally {
    await context.close();
    await browser.close();
  }
}

export default globalSetup;