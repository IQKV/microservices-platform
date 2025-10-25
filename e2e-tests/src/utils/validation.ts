/**
 * Validation utilities for API request/response data
 * Uses Joi for schema validation
 */

import Joi from 'joi';
import { 
  AuthResponse, 
  UserData, 
  BookData, 
  TenantData, 
  ErrorResponse,
  ServiceErrorResponse,
  PaginatedResponse,
  HealthCheckResponse,
  ValidationError,
  UserContext,
  CreateUserRequest,
  UpdateUserRequest,
  BookSearchCriteria,
  CreateTenantRequest,
  UpdateTenantRequest,
  VerificationStatusResponse,
  CategoryResponse,
  SearchMetadata,
  FilterMetadata,
  ApiVersionInfo
} from '../types/api-responses.js';

// Common validation patterns
const commonPatterns = {
  id: Joi.number().integer().positive(),
  uuid: Joi.string().uuid(),
  email: Joi.string().email(),
  username: Joi.string().min(3).max(50).pattern(/^[a-zA-Z0-9_-]+$/),
  password: Joi.string().min(8).max(128),
  timestamp: Joi.string().isoDate(),
  url: Joi.string().uri(),
  tenantId: Joi.string().min(1).max(100),
  correlationId: Joi.string().min(1).max(100),
  currency: Joi.string().length(3).uppercase(), // ISO 4217
  isbn: Joi.string().pattern(
    /^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$/
  )
};

// Authentication schemas
export const loginCredentialsSchema = Joi.object({
  username: commonPatterns.username.required(),
  password: commonPatterns.password.required(),
  tenantId: commonPatterns.tenantId.optional()
});

export const authTokensSchema = Joi.object({
  accessToken: Joi.string().required(),
  refreshToken: Joi.string().required(),
  tokenType: Joi.string().valid('Bearer').required(),
  expiresIn: Joi.number().integer().positive().required(),
  refreshExpiresIn: Joi.number().integer().positive().required(),
  scope: Joi.string().optional()
});

export const userDataSchema = Joi.object({
  id: commonPatterns.id.required(),
  username: commonPatterns.username.required(),
  email: commonPatterns.email.required(),
  firstName: Joi.string().min(1).max(100).optional(),
  lastName: Joi.string().min(1).max(100).optional(),
  roles: Joi.array().items(Joi.string().min(1).max(50)).required(),
  permissions: Joi.array().items(Joi.string().min(1).max(100)).required(),
  department: Joi.string().min(1).max(100).optional(),
  organizationId: Joi.string().min(1).max(100).optional(),
  tenantId: commonPatterns.tenantId.optional(),
  enabled: Joi.boolean().required(),
  emailVerified: Joi.boolean().required(),
  createdAt: commonPatterns.timestamp.required(),
  updatedAt: commonPatterns.timestamp.required()
});

export const authResponseSchema = Joi.object({
  user: userDataSchema.required(),
  tokens: authTokensSchema.required(),
  tenant: Joi.object({
    id: commonPatterns.tenantId.required(),
    name: Joi.string().min(1).max(200).required(),
    domain: Joi.string().domain().optional(),
    subdomain: Joi.string().min(1).max(100).optional(),
    enabled: Joi.boolean().required(),
    settings: Joi.object().unknown(true).required(),
    createdAt: commonPatterns.timestamp.required(),
    updatedAt: commonPatterns.timestamp.required()
  }).optional()
});

// User registration schema
export const userRegistrationSchema = Joi.object({
  username: commonPatterns.username.required(),
  email: commonPatterns.email.required(),
  password: commonPatterns.password.required(),
  firstName: Joi.string().min(1).max(100).optional(),
  lastName: Joi.string().min(1).max(100).optional(),
  tenantId: commonPatterns.tenantId.optional()
});

// Book schemas
export const bookDataSchema = Joi.object({
  id: commonPatterns.id.required(),
  title: Joi.string().min(1).max(500).required(),
  author: Joi.string().min(1).max(200).required(),
  isbn: commonPatterns.isbn.required(),
  description: Joi.string().max(2000).optional(),
  price: Joi.number().positive().precision(2).required(),
  currency: commonPatterns.currency.required(),
  stock: Joi.number().integer().min(0).required(),
  category: Joi.string().min(1).max(100).required(),
  tags: Joi.array().items(Joi.string().min(1).max(50)).required(),
  tenantId: commonPatterns.tenantId.required(),
  createdAt: commonPatterns.timestamp.required(),
  updatedAt: commonPatterns.timestamp.required()
});

export const createBookSchema = Joi.object({
  title: Joi.string().min(1).max(500).required(),
  author: Joi.string().min(1).max(200).required(),
  isbn: commonPatterns.isbn.required(),
  description: Joi.string().max(2000).optional(),
  price: Joi.number().positive().precision(2).required(),
  currency: commonPatterns.currency.required(),
  stock: Joi.number().integer().min(0).required(),
  category: Joi.string().min(1).max(100).required(),
  tags: Joi.array().items(Joi.string().min(1).max(50)).optional().default([])
});

// Tenant schema
export const tenantDataSchema = Joi.object({
  id: commonPatterns.tenantId.required(),
  name: Joi.string().min(1).max(200).required(),
  domain: Joi.string().domain().optional(),
  subdomain: Joi.string().min(1).max(100).optional(),
  enabled: Joi.boolean().required(),
  settings: Joi.object().unknown(true).required(),
  createdAt: commonPatterns.timestamp.required(),
  updatedAt: commonPatterns.timestamp.required()
});

// Error response schema
export const errorResponseSchema = Joi.object({
  error: Joi.object({
    code: Joi.string().min(1).max(100).required(),
    message: Joi.string().min(1).max(500).required(),
    details: Joi.string().max(1000).required(),
    timestamp: commonPatterns.timestamp.required(),
    path: Joi.string().min(1).max(500).required(),
    method: Joi.string().valid('GET', 'POST', 'PUT', 'PATCH', 'DELETE').required(),
    correlationId: commonPatterns.correlationId.required(),
    requestId: Joi.string().min(1).max(100).optional(),
    fields: Joi.array().items(Joi.object({
      field: Joi.string().min(1).max(100).required(),
      message: Joi.string().min(1).max(200).required(),
      rejectedValue: Joi.any().optional(),
      code: Joi.string().min(1).max(50).optional()
    })).optional()
  }).required()
});

// Paginated response schema
export const paginatedResponseSchema = <T>(itemSchema: Joi.Schema<T>) => Joi.object({
  content: Joi.array().items(itemSchema).required(),
  page: Joi.object({
    number: Joi.number().integer().min(0).required(),
    size: Joi.number().integer().positive().required(),
    totalElements: Joi.number().integer().min(0).required(),
    totalPages: Joi.number().integer().min(0).required()
  }).required(),
  sort: Joi.object({
    sorted: Joi.boolean().required(),
    unsorted: Joi.boolean().required(),
    empty: Joi.boolean().required()
  }).required(),
  first: Joi.boolean().required(),
  last: Joi.boolean().required(),
  numberOfElements: Joi.number().integer().min(0).required(),
  empty: Joi.boolean().required()
});

// Health check schema
export const healthCheckResponseSchema = Joi.object({
  status: Joi.string().valid('UP', 'DOWN', 'OUT_OF_SERVICE', 'UNKNOWN').required(),
  components: Joi.object().pattern(
    Joi.string(),
    Joi.object({
      status: Joi.string().valid('UP', 'DOWN', 'OUT_OF_SERVICE', 'UNKNOWN').required(),
      details: Joi.object().unknown(true).optional()
    })
  ).required(),
  groups: Joi.array().items(Joi.string()).optional()
});

// Validation utility functions
export class ValidationUtils {
  /**
   * Validate data against a Joi schema
   */
  static validate<T>(data: unknown, schema: Joi.Schema<T>): T {
    const { error, value } = schema.validate(data, {
      abortEarly: false,
      stripUnknown: true,
      convert: true
    });

    if (error) {
      const validationErrors: ValidationError[] = error.details.map(detail => ({
        field: detail.path.join('.'),
        message: detail.message,
        rejectedValue: detail.context?.value,
        code: detail.type
      }));

      throw new ApiValidationError(`Validation failed: ${error.message}`, validationErrors);
    }

    return value;
  }

  /**
   * Validate authentication response
   */
  static validateAuthResponse(data: unknown): AuthResponse {
    return this.validate(data, authResponseSchema);
  }

  /**
   * Validate user data
   */
  static validateUserData(data: unknown): UserData {
    return this.validate(data, userDataSchema);
  }

  /**
   * Validate book data
   */
  static validateBookData(data: unknown): BookData {
    return this.validate(data, bookDataSchema);
  }

  /**
   * Validate tenant data
   */
  static validateTenantData(data: unknown): TenantData {
    return this.validate(data, tenantDataSchema);
  }

  /**
   * Validate error response
   */
  static validateErrorResponse(data: unknown): ErrorResponse {
    return this.validate(data, errorResponseSchema);
  }

  /**
   * Validate paginated response
   */
  static validatePaginatedResponse<T>(data: unknown, itemSchema: Joi.Schema<T>): PaginatedResponse<T> {
    return this.validate(data, paginatedResponseSchema(itemSchema));
  }

  /**
   * Validate health check response
   */
  static validateHealthCheckResponse(data: unknown): HealthCheckResponse {
    return this.validate(data, healthCheckResponseSchema);
  }

  /**
   * Check if data matches schema without throwing
   */
  static isValid<T>(data: unknown, schema: Joi.Schema<T>): boolean {
    const { error } = schema.validate(data);
    return !error;
  }

  /**
   * Get validation errors without throwing
   */
  static getValidationErrors<T>(data: unknown, schema: Joi.Schema<T>): ValidationError[] {
    const { error } = schema.validate(data, { abortEarly: false });
    
    if (!error) {
      return [];
    }

    return error.details.map(detail => ({
      field: detail.path.join('.'),
      message: detail.message,
      rejectedValue: detail.context?.value,
      code: detail.type
    }));
  }

  /**
   * Validate user context
   */
  static validateUserContext(data: unknown): UserContext {
    return this.validate(data, userContextSchema);
  }

  /**
   * Validate service error response
   */
  static validateServiceErrorResponse(data: unknown): ServiceErrorResponse {
    return this.validate(data, serviceErrorResponseSchema);
  }

  /**
   * Validate book search criteria
   */
  static validateBookSearchCriteria(data: unknown): BookSearchCriteria {
    return this.validate(data, bookSearchCriteriaSchema);
  }

  /**
   * Validate create user request
   */
  static validateCreateUserRequest(data: unknown): CreateUserRequest {
    return this.validate(data, createUserRequestSchema);
  }

  /**
   * Validate update user request
   */
  static validateUpdateUserRequest(data: unknown): UpdateUserRequest {
    return this.validate(data, updateUserRequestSchema);
  }

  /**
   * Validate create tenant request
   */
  static validateCreateTenantRequest(data: unknown): CreateTenantRequest {
    return this.validate(data, createTenantRequestSchema);
  }

  /**
   * Validate update tenant request
   */
  static validateUpdateTenantRequest(data: unknown): UpdateTenantRequest {
    return this.validate(data, updateTenantRequestSchema);
  }

  /**
   * Validate verification status response
   */
  static validateVerificationStatusResponse(data: unknown): VerificationStatusResponse {
    return this.validate(data, verificationStatusResponseSchema);
  }

  /**
   * Validate category response
   */
  static validateCategoryResponse(data: unknown): CategoryResponse {
    return this.validate(data, categoryResponseSchema);
  }

  /**
   * Validate search metadata
   */
  static validateSearchMetadata(data: unknown): SearchMetadata {
    return this.validate(data, searchMetadataSchema);
  }

  /**
   * Validate filter metadata
   */
  static validateFilterMetadata(data: unknown): FilterMetadata {
    return this.validate(data, filterMetadataSchema);
  }

  /**
   * Validate API version info
   */
  static validateApiVersionInfo(data: unknown): ApiVersionInfo {
    return this.validate(data, apiVersionInfoSchema);
  }
}

// Custom validation error class
export class ApiValidationError extends Error {
  public readonly validationErrors: ValidationError[];

  constructor(message: string, validationErrors: ValidationError[]) {
    super(message);
    this.name = 'ApiValidationError';
    this.validationErrors = validationErrors;
  }
}

// Additional validation schemas for new types

// Service error response schema (alternative format)
export const serviceErrorResponseSchema = Joi.object({
  code: Joi.string().min(1).max(100).required(),
  message: Joi.string().min(1).max(500).required(),
  details: Joi.string().max(1000).required(),
  timestamp: commonPatterns.timestamp.required(),
  path: Joi.string().min(1).max(500).required(),
  method: Joi.string().valid('GET', 'POST', 'PUT', 'PATCH', 'DELETE').required(),
  correlationId: commonPatterns.correlationId.required(),
  requestId: Joi.string().min(1).max(100).optional(),
  fields: Joi.array().items(Joi.object({
    field: Joi.string().min(1).max(100).required(),
    rejectedValue: Joi.any().optional(),
    message: Joi.string().min(1).max(200).required()
  })).optional()
});

// User context schema for JWT tokens
export const userContextSchema = Joi.object({
  userId: commonPatterns.id.required(),
  username: commonPatterns.username.required(),
  email: commonPatterns.email.required(),
  roles: Joi.array().items(Joi.string().min(1).max(50)).required(),
  permissions: Joi.array().items(Joi.string().min(1).max(100)).required(),
  firstName: Joi.string().min(1).max(100).optional(),
  lastName: Joi.string().min(1).max(100).optional(),
  tenantId: commonPatterns.tenantId.optional(),
  department: Joi.string().min(1).max(100).optional(),
  organizationId: Joi.string().min(1).max(100).optional(),
  customClaims: Joi.object().unknown(true).optional()
});

// Create user request schema
export const createUserRequestSchema = Joi.object({
  username: commonPatterns.username.required(),
  email: commonPatterns.email.required(),
  password: commonPatterns.password.required(),
  firstName: Joi.string().min(1).max(100).optional(),
  lastName: Joi.string().min(1).max(100).optional(),
  roles: Joi.array().items(Joi.string().min(1).max(50)).optional(),
  enabled: Joi.boolean().optional().default(true),
  tenantId: commonPatterns.tenantId.optional()
});

// Update user request schema
export const updateUserRequestSchema = Joi.object({
  firstName: Joi.string().min(1).max(100).optional(),
  lastName: Joi.string().min(1).max(100).optional(),
  email: commonPatterns.email.optional(),
  roles: Joi.array().items(Joi.string().min(1).max(50)).optional(),
  enabled: Joi.boolean().optional(),
  department: Joi.string().min(1).max(100).optional()
});

// Book search criteria schema
export const bookSearchCriteriaSchema = Joi.object({
  title: Joi.string().min(1).max(500).optional(),
  author: Joi.string().min(1).max(200).optional(),
  category: Joi.string().min(1).max(100).optional(),
  minPrice: Joi.number().positive().optional(),
  maxPrice: Joi.number().positive().optional(),
  availableOnly: Joi.boolean().optional(),
  tags: Joi.array().items(Joi.string().min(1).max(50)).optional(),
  inStock: Joi.boolean().optional()
});

// Update book request schema
export const updateBookRequestSchema = Joi.object({
  title: Joi.string().min(1).max(500).optional(),
  author: Joi.string().min(1).max(200).optional(),
  isbn: commonPatterns.isbn.optional(),
  description: Joi.string().max(2000).optional(),
  price: Joi.number().positive().precision(2).optional(),
  currency: commonPatterns.currency.optional(),
  stock: Joi.number().integer().min(0).optional(),
  category: Joi.string().min(1).max(100).optional(),
  tags: Joi.array().items(Joi.string().min(1).max(50)).optional()
});

// Create tenant request schema
export const createTenantRequestSchema = Joi.object({
  name: Joi.string().min(1).max(200).required(),
  domain: Joi.string().domain().optional(),
  subdomain: Joi.string().min(1).max(100).optional(),
  enabled: Joi.boolean().optional().default(true),
  settings: Joi.object().unknown(true).optional().default({})
});

// Update tenant request schema
export const updateTenantRequestSchema = Joi.object({
  name: Joi.string().min(1).max(200).optional(),
  domain: Joi.string().domain().optional(),
  subdomain: Joi.string().min(1).max(100).optional(),
  enabled: Joi.boolean().optional(),
  settings: Joi.object().unknown(true).optional()
});

// Email verification request schema
export const emailVerificationRequestSchema = Joi.object({
  token: Joi.string().min(1).max(500).required()
});

// Resend verification request schema
export const resendVerificationRequestSchema = Joi.object({
  email: commonPatterns.email.required()
});

// Verification status response schema
export const verificationStatusResponseSchema = Joi.object({
  verified: Joi.boolean().required(),
  message: Joi.string().min(1).max(200).required(),
  user: userDataSchema.optional()
});

// Update inventory request schema
export const updateInventoryRequestSchema = Joi.object({
  stock: Joi.number().integer().min(0).required()
});

// Bulk inventory request schema
export const bulkInventoryRequestSchema = Joi.object({
  updates: Joi.array().items(Joi.object({
    bookId: commonPatterns.id.required(),
    stock: Joi.number().integer().min(0).required()
  })).min(1).required()
});

// Category response schema
export const categoryResponseSchema = Joi.object({
  id: commonPatterns.id.required(),
  name: Joi.string().min(1).max(100).required(),
  description: Joi.string().max(500).optional(),
  bookCount: Joi.number().integer().min(0).optional()
});

// Search metadata schema
export const searchMetadataSchema = Joi.object({
  query: Joi.string().optional(),
  totalResults: Joi.number().integer().min(0).required(),
  searchTime: Joi.number().positive().required(),
  filters: Joi.object().unknown(true).optional()
});

// Filter metadata schema
export const filterMetadataSchema = Joi.object({
  availableCategories: Joi.array().items(Joi.string()).required(),
  priceRange: Joi.object({
    min: Joi.number().min(0).required(),
    max: Joi.number().positive().required()
  }).required(),
  availableAuthors: Joi.array().items(Joi.string()).required(),
  totalBooks: Joi.number().integer().min(0).required()
});

// API version info schema
export const apiVersionInfoSchema = Joi.object({
  version: Joi.string().pattern(/^\d+\.\d+\.\d+$/).required(),
  supportedVersions: Joi.array().items(Joi.string().pattern(/^\d+\.\d+\.\d+$/)).required(),
  deprecatedVersions: Joi.array().items(Joi.string().pattern(/^\d+\.\d+\.\d+$/)).required(),
  latestVersion: Joi.string().pattern(/^\d+\.\d+\.\d+$/).required()
});

// Export commonly used schemas
export const schemas = {
  loginCredentials: loginCredentialsSchema,
  userRegistration: userRegistrationSchema,
  authResponse: authResponseSchema,
  authTokens: authTokensSchema,
  userData: userDataSchema,
  userContext: userContextSchema,
  createUser: createUserRequestSchema,
  updateUser: updateUserRequestSchema,
  bookData: bookDataSchema,
  createBook: createBookSchema,
  updateBook: updateBookRequestSchema,
  bookSearchCriteria: bookSearchCriteriaSchema,
  tenantData: tenantDataSchema,
  createTenant: createTenantRequestSchema,
  updateTenant: updateTenantRequestSchema,
  errorResponse: errorResponseSchema,
  serviceErrorResponse: serviceErrorResponseSchema,
  healthCheckResponse: healthCheckResponseSchema,
  paginatedResponse: paginatedResponseSchema,
  emailVerification: emailVerificationRequestSchema,
  resendVerification: resendVerificationRequestSchema,
  verificationStatus: verificationStatusResponseSchema,
  updateInventory: updateInventoryRequestSchema,
  bulkInventory: bulkInventoryRequestSchema,
  categoryResponse: categoryResponseSchema,
  searchMetadata: searchMetadataSchema,
  filterMetadata: filterMetadataSchema,
  apiVersionInfo: apiVersionInfoSchema
};