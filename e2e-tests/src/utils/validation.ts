/**
 * Validation utilities for API request/response data
 * Uses Joi for schema validation
 */

import Joi from 'joi';
import { 
  AuthResponse, 
  UserData, 
  AuthTokens, 
  BookData, 
  TenantData, 
  ErrorResponse,
  PaginatedResponse,
  HealthCheckResponse,
  ValidationError
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
  isbn: Joi.string().pattern(/^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$/)
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
  static validate<T>(data: any, schema: Joi.Schema<T>): T {
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
  static validateAuthResponse(data: any): AuthResponse {
    return this.validate(data, authResponseSchema);
  }

  /**
   * Validate user data
   */
  static validateUserData(data: any): UserData {
    return this.validate(data, userDataSchema);
  }

  /**
   * Validate book data
   */
  static validateBookData(data: any): BookData {
    return this.validate(data, bookDataSchema);
  }

  /**
   * Validate tenant data
   */
  static validateTenantData(data: any): TenantData {
    return this.validate(data, tenantDataSchema);
  }

  /**
   * Validate error response
   */
  static validateErrorResponse(data: any): ErrorResponse {
    return this.validate(data, errorResponseSchema);
  }

  /**
   * Validate paginated response
   */
  static validatePaginatedResponse<T>(data: any, itemSchema: Joi.Schema<T>): PaginatedResponse<T> {
    return this.validate(data, paginatedResponseSchema(itemSchema));
  }

  /**
   * Validate health check response
   */
  static validateHealthCheckResponse(data: any): HealthCheckResponse {
    return this.validate(data, healthCheckResponseSchema);
  }

  /**
   * Check if data matches schema without throwing
   */
  static isValid<T>(data: any, schema: Joi.Schema<T>): boolean {
    const { error } = schema.validate(data);
    return !error;
  }

  /**
   * Get validation errors without throwing
   */
  static getValidationErrors<T>(data: any, schema: Joi.Schema<T>): ValidationError[] {
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

// Export commonly used schemas
export const schemas = {
  loginCredentials: loginCredentialsSchema,
  userRegistration: userRegistrationSchema,
  authResponse: authResponseSchema,
  authTokens: authTokensSchema,
  userData: userDataSchema,
  bookData: bookDataSchema,
  createBook: createBookSchema,
  tenantData: tenantDataSchema,
  errorResponse: errorResponseSchema,
  healthCheckResponse: healthCheckResponseSchema,
  paginatedResponse: paginatedResponseSchema
};