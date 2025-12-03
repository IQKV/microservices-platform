/**
 * API response type definitions for iqscaffold microservices platform
 * Based on the Spring Boot error response format from the steering rules
 */

// Base API response structure
export interface ApiResponse<T = unknown> {
  status: number;
  headers: Record<string, string>;
  data: T;
  duration: number;
  correlationId?: string;
  requestId?: string;
}

// Standard error response format matching Spring Boot platform
export interface ErrorResponse {
  error: {
    code: string;
    message: string;
    details: string;
    timestamp: string;
    path: string;
    method: string;
    correlationId: string;
    requestId?: string;
    fields?: ValidationError[];
  };
}

// Alternative error response format (direct format from services)
export interface ServiceErrorResponse {
  code: string;
  message: string;
  details: string;
  timestamp: string;
  path: string;
  method: string;
  correlationId: string;
  requestId?: string;
  fields?: FieldError[];
}

// Field error for service responses
export interface FieldError {
  field: string;
  rejectedValue?: unknown;
  message: string;
}

// Validation error for field-specific errors
export interface ValidationError {
  field: string;
  message: string;
  rejectedValue?: unknown;
  code?: string;
}

// Authentication related responses
export interface LoginCredentials {
  username: string;
  password: string;
  tenantId?: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  refreshExpiresIn: number;
  scope?: string;
}

export interface AuthResponse {
  user: UserData;
  tokens: AuthTokens;
  tenant?: TenantData;
}

export interface UserData {
  id: number;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  roles: string[];
  permissions: string[];
  department?: string;
  organizationId?: string;
  tenantId?: string;
  enabled: boolean;
  emailVerified: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UserRegistrationData {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
  tenantId?: string;
}

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  roles: string[];
  enabled: boolean;
  emailVerified: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface VerificationResponse {
  success: boolean;
  message: string;
  user?: UserResponse;
}

// Tenant related responses
export interface TenantData {
  id: string;
  name: string;
  domain?: string;
  subdomain?: string;
  enabled: boolean;
  settings: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
}

// Paginated response structure
export interface PaginatedResponse<T> {
  content: T[];
  page: {
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
  sort: {
    sorted: boolean;
    unsorted: boolean;
    empty: boolean;
  };
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}

// Health check response
export interface HealthCheckResponse {
  status: 'UP' | 'DOWN' | 'OUT_OF_SERVICE' | 'UNKNOWN';
  components: Record<string, {
    status: 'UP' | 'DOWN' | 'OUT_OF_SERVICE' | 'UNKNOWN';
    details?: Record<string, any>;
  }>;
  groups?: string[];
}

// Request configuration for API client
export interface RequestConfig {
  method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  url: string;
  headers?: Record<string, string>;
  data?: unknown;
  params?: Record<string, string | number | boolean>;
  timeout?: number;
  retries?: number;
  validateStatus?: (status: number) => boolean;
}

// Response validation schema types
export interface ResponseSchema {
  statusCode?: number | number[];
  headers?: Record<string, string | RegExp>;
  body?: unknown; // Joi schema or validation function
}

// Error types for API client
export enum ApiErrorType {
  NETWORK_ERROR = 'NETWORK_ERROR',
  TIMEOUT_ERROR = 'TIMEOUT_ERROR',
  AUTHENTICATION_ERROR = 'AUTHENTICATION_ERROR',
  AUTHORIZATION_ERROR = 'AUTHORIZATION_ERROR',
  VALIDATION_ERROR = 'VALIDATION_ERROR',
  NOT_FOUND_ERROR = 'NOT_FOUND_ERROR',
  RATE_LIMIT_ERROR = 'RATE_LIMIT_ERROR',
  SERVER_ERROR = 'SERVER_ERROR',
  UNKNOWN_ERROR = 'UNKNOWN_ERROR'
}

export class ApiError extends Error {
  public readonly type: ApiErrorType;
  public readonly status?: number;
  public readonly response?: ApiResponse;
  public readonly correlationId?: string;
  public readonly requestId?: string;

  constructor(
    message: string,
    type: ApiErrorType,
    responseStatus?: number,
    response?: ApiResponse,
    correlationId?: string,
    requestId?: string
  ) {
    super(message);
    this.name = 'ApiError';
    this.type = type;
    if (responseStatus !== undefined) this.status = responseStatus;
    if (response !== undefined) this.response = response;
    if (correlationId !== undefined) this.correlationId = correlationId;
    if (requestId !== undefined) this.requestId = requestId;
  }
}

// Token refresh response
export interface TokenRefreshResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  refreshExpiresIn: number;
}

// Logout response
export interface LogoutResponse {
  success: boolean;
  message: string;
}

// Email verification request
export interface EmailVerificationRequest {
  token: string;
}

// Resend verification request
export interface ResendVerificationRequest {
  email: string;
}

// Verification status response
export interface VerificationStatusResponse {
  verified: boolean;
  message: string;
  user?: UserResponse;
}

// User context for JWT tokens
export interface UserContext {
  userId: number;
  username: string;
  email: string;
  roles: string[];
  permissions: string[];
  firstName?: string;
  lastName?: string;
  tenantId?: string;
  department?: string;
  organizationId?: string;
  customClaims?: Record<string, unknown>;
}

// Create user request (admin only)
export interface CreateUserRequest {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
  roles?: string[];
  enabled?: boolean;
  tenantId?: string;
}

// Update user request
export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
  roles?: string[];
  enabled?: boolean;
  department?: string;
}

// Tenant creation request
export interface CreateTenantRequest {
  name: string;
  domain?: string;
  subdomain?: string;
  enabled?: boolean;
  settings?: Record<string, unknown>;
}

// Tenant update request
export interface UpdateTenantRequest {
  name?: string;
  domain?: string;
  subdomain?: string;
  enabled?: boolean;
  settings?: Record<string, unknown>;
}

// Rate limiting response headers
export interface RateLimitHeaders {
  'X-RateLimit-Limit': string;
  'X-RateLimit-Remaining': string;
  'X-RateLimit-Reset': string;
  'X-RateLimit-Retry-After'?: string;
}

// CORS headers
export interface CorsHeaders {
  'Access-Control-Allow-Origin': string;
  'Access-Control-Allow-Methods': string;
  'Access-Control-Allow-Headers': string;
  'Access-Control-Allow-Credentials': string;
  'Access-Control-Max-Age': string;
}

// Security headers
export interface SecurityHeaders {
  'X-Content-Type-Options': string;
  'X-Frame-Options': string;
  'X-XSS-Protection': string;
  'Strict-Transport-Security': string;
  'Content-Security-Policy'?: string;
}

// Common response headers
export interface CommonResponseHeaders extends Partial<RateLimitHeaders>, Partial<SecurityHeaders> {
  'Content-Type': string;
  'X-Correlation-ID'?: string;
  'X-Request-ID'?: string;
  'X-Tenant-ID'?: string;
  'Cache-Control'?: string;
  'ETag'?: string;
  'Last-Modified'?: string;
}
