/**
 * Utility exports for Playwright API testing
 */

// API Client exports
export { ApiClient, createApiClient } from './api-client.js';
export { TypedApiClient } from './typed-api-client.js';

// Validation exports
export { 
  ValidationUtils, 
  ApiValidationError,
  schemas 
} from './validation.js';

// Re-export types for convenience
export type {
  PaginationParams,
  BookSearchParams,
  UserSearchParams
} from './typed-api-client.js';

export type { ApiClientConfig } from './api-client.js';

export type {
  ApiResponse,
  ApiError,
  RequestConfig,
  AuthTokens,
  LoginCredentials,
  AuthResponse,
  UserData,
  UserRegistrationData,
  BookData,
  TenantData,
  PaginatedResponse,
  HealthCheckResponse,
  ErrorResponse,
  ValidationError
} from '../types/api-responses.js';