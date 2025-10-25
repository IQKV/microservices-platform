/**
 * API response type definitions for Gripday microservices platform
 * Based on the Spring Boot error response format from the steering rules
 */

// Base API response structure
export interface ApiResponse<T = any> {
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

// Validation error for field-specific errors
export interface ValidationError {
  field: string;
  message: string;
  rejectedValue?: any;
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
  settings: Record<string, any>;
  createdAt: string;
  updatedAt: string;
}

// Book related responses (for Bookstore service)
export interface BookData {
  id: number;
  title: string;
  author: string;
  isbn: string;
  description?: string;
  price: number;
  currency: string;
  stock: number;
  category: string;
  tags: string[];
  tenantId: string;
  createdAt: string;
  updatedAt: string;
}

export interface BookResponse {
  id: number;
  title: string;
  author: string;
  isbn: string;
  description?: string;
  price: number;
  currency: string;
  stock: number;
  category: string;
  tags: string[];
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
  data?: any;
  params?: Record<string, string | number | boolean>;
  timeout?: number;
  retries?: number;
  validateStatus?: (status: number) => boolean;
}

// Response validation schema types
export interface ResponseSchema {
  statusCode?: number | number[];
  headers?: Record<string, string | RegExp>;
  body?: any; // Joi schema or validation function
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
    status?: number,
    response?: ApiResponse,
    correlationId?: string,
    requestId?: string
  ) {
    super(message);
    this.name = 'ApiError';
    this.type = type;
    if (status !== undefined) this.status = status;
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