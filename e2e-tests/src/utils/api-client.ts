/**
 * Authenticated HTTP client for Playwright API testing
 * Provides JWT token management, request/response handling, and retry logic
 */

import { APIRequestContext, request } from '@playwright/test';
import { createLogger, Logger } from 'winston';
import {
    ApiResponse,
    ApiError,
    ApiErrorType,
    RequestConfig,
    AuthTokens,
    LoginCredentials,
    AuthResponse,
    TokenRefreshResponse,
    ErrorResponse
} from '../types/api-responses.js';
import { TestEnvironmentConfig } from '../types/environment.js';

export interface ApiClientConfig {
    baseUrl: string;
    timeout: number;
    retries: number;
    debug: boolean;
    tenantId?: string;
    correlationIdHeader?: string;
    logger?: Logger;
}

export class ApiClient {
    private context: APIRequestContext | null = null;
    private config: ApiClientConfig;
    private authTokens: AuthTokens | null = null;
    private tenantId: string | null = null;
    private logger: Logger;
    private tokenRefreshPromise: Promise<AuthTokens> | null = null;

    constructor(config: ApiClientConfig) {
        this.config = {
            correlationIdHeader: 'X-Correlation-ID',
            ...config
        };

        this.logger = config.logger || createLogger({
            level: config.debug ? 'debug' : 'info',
            format: require('winston').format.combine(
                require('winston').format.timestamp(),
                require('winston').format.json()
            ),
            transports: [
                new (require('winston').transports.Console)()
            ]
        });
    }

    /**
     * Initialize the API client with Playwright request context
     */
    async initialize(): Promise<void> {
        this.context = await request.newContext({
            baseURL: this.config.baseUrl,
            timeout: this.config.timeout,
            ignoreHTTPSErrors: true
        });

        this.logger.info('API client initialized', {
            baseUrl: this.config.baseUrl,
            timeout: this.config.timeout
        });
    }

    /**
     * Authenticate with username/password and store tokens
     */
    async authenticate(credentials: LoginCredentials): Promise<AuthResponse> {
        if (!this.context) {
            throw new Error('API client not initialized. Call initialize() first.');
        }

        const startTime = Date.now();

        try {
            this.logger.debug('Authenticating user', {
                username: credentials.username,
                tenantId: credentials.tenantId
            });

            const headers: Record<string, string> = {
                'Content-Type': 'application/json'
            };

            if (credentials.tenantId) {
                headers['X-Tenant-ID'] = credentials.tenantId;
                this.tenantId = credentials.tenantId;
            }

            const response = await this.context.post('/api/v1/auth/login', {
                headers,
                data: {
                    username: credentials.username,
                    password: credentials.password
                }
            });

            const duration = Date.now() - startTime;
            const responseData = await response.json();

            if (!response.ok()) {
                throw this.createApiError(response.status(), responseData, duration);
            }

            const authResponse: AuthResponse = responseData;
            this.authTokens = authResponse.tokens;

            this.logger.info('Authentication successful', {
                username: credentials.username,
                userId: authResponse.user.id,
                duration
            });

            return authResponse;
        } catch (error) {
            const duration = Date.now() - startTime;
            this.logger.error('Authentication failed', {
                username: credentials.username,
                error: error instanceof Error ? error.message : String(error),
                duration
            });
            throw error;
        }
    }

    /**
     * Set authentication tokens manually
     */
    setAuthTokens(tokens: AuthTokens): void {
        this.authTokens = tokens;
        this.logger.debug('Auth tokens set manually');
    }

    /**
     * Set tenant ID for multi-tenant requests
     */
    setTenant(tenantId: string): void {
        this.tenantId = tenantId;
        this.logger.debug('Tenant ID set', { tenantId });
    }

    /**
     * Make an authenticated HTTP request with automatic token refresh
     */
    async request<T = any>(config: RequestConfig): Promise<ApiResponse<T>> {
        if (!this.context) {
            throw new Error('API client not initialized. Call initialize() first.');
        }

        let attempt = 0;
        const maxRetries = config.retries ?? this.config.retries;

        while (attempt <= maxRetries) {
            try {
                return await this.executeRequest<T>(config);
            } catch (error) {
                attempt++;

                if (error instanceof ApiError) {
                    // Handle token expiration
                    if (error.type === ApiErrorType.AUTHENTICATION_ERROR &&
                        error.status === 401 &&
                        this.authTokens?.refreshToken &&
                        attempt <= maxRetries) {

                        this.logger.debug('Token expired, attempting refresh', { attempt });

                        try {
                            await this.refreshTokens();
                            continue; // Retry the request with new token
                        } catch (refreshError) {
                            this.logger.error('Token refresh failed', {
                                error: refreshError instanceof Error ? refreshError.message : String(refreshError)
                            });
                            throw refreshError;
                        }
                    }

                    // Handle network errors with exponential backoff
                    if ((error.type === ApiErrorType.NETWORK_ERROR ||
                        error.type === ApiErrorType.TIMEOUT_ERROR) &&
                        attempt <= maxRetries) {

                        const backoffDelay = Math.min(1000 * Math.pow(2, attempt - 1), 10000);
                        this.logger.debug('Network error, retrying with backoff', {
                            attempt,
                            backoffDelay,
                            error: error.message
                        });

                        await this.sleep(backoffDelay);
                        continue;
                    }
                }

                // If we've exhausted retries or it's not a retryable error, throw
                if (attempt > maxRetries) {
                    this.logger.error('Request failed after all retries', {
                        url: config.url,
                        method: config.method,
                        attempts: attempt,
                        error: error instanceof Error ? error.message : String(error)
                    });
                }

                throw error;
            }
        }

        throw new Error('Unexpected error in request retry loop');
    }

    /**
     * Execute a single HTTP request
     */
    private async executeRequest<T>(config: RequestConfig): Promise<ApiResponse<T>> {
        const startTime = Date.now();
        const correlationId = this.generateCorrelationId();

        // Build headers
        const headers: Record<string, string> = {
            'Content-Type': 'application/json',
            ...config.headers
        };

        // Add correlation ID
        if (this.config.correlationIdHeader) {
            headers[this.config.correlationIdHeader] = correlationId;
        }

        // Add tenant ID if set
        if (this.tenantId) {
            headers['X-Tenant-ID'] = this.tenantId;
        }

        // Add authentication token
        if (this.authTokens?.accessToken) {
            headers['Authorization'] = `Bearer ${this.authTokens.accessToken}`;
        }

        this.logger.debug('Making HTTP request', {
            method: config.method,
            url: config.url,
            correlationId,
            tenantId: this.tenantId,
            hasAuth: !!this.authTokens?.accessToken
        });

        try {
            const requestOptions: any = {
                headers,
                timeout: config.timeout ?? this.config.timeout
            };

            // Add request body for non-GET requests
            if (config.data && config.method !== 'GET') {
                requestOptions.data = config.data;
            }

            // Add query parameters
            if (config.params) {
                requestOptions.params = config.params;
            }

            let response;
            switch (config.method) {
                case 'GET':
                    response = await this.context!.get(config.url, requestOptions);
                    break;
                case 'POST':
                    response = await this.context!.post(config.url, requestOptions);
                    break;
                case 'PUT':
                    response = await this.context!.put(config.url, requestOptions);
                    break;
                case 'PATCH':
                    response = await this.context!.patch(config.url, requestOptions);
                    break;
                case 'DELETE':
                    response = await this.context!.delete(config.url, requestOptions);
                    break;
                default:
                    throw new Error(`Unsupported HTTP method: ${config.method}`);
            }

            const duration = Date.now() - startTime;
            const responseHeaders = response.headers();
            const status = response.status();

            let responseData;
            try {
                responseData = await response.json();
            } catch {
                // Handle non-JSON responses
                responseData = await response.text();
            }

            // Check if response should be considered successful
            const isSuccess = config.validateStatus ?
                config.validateStatus(status) :
                (status >= 200 && status < 300);

            if (!isSuccess) {
                throw this.createApiError(status, responseData, duration, correlationId);
            }

            const apiResponse: ApiResponse<T> = {
                status,
                headers: responseHeaders,
                data: responseData,
                duration,
                ...(correlationId && { correlationId }),
                ...(responseHeaders['x-request-id'] && { requestId: responseHeaders['x-request-id'] })
            };

            this.logger.debug('HTTP request successful', {
                method: config.method,
                url: config.url,
                status,
                duration,
                correlationId
            });

            return apiResponse;

        } catch (error) {
            const duration = Date.now() - startTime;

            this.logger.error('HTTP request failed', {
                method: config.method,
                url: config.url,
                duration,
                correlationId,
                error: error instanceof Error ? error.message : String(error)
            });

            // Convert Playwright errors to our ApiError format
            if (error instanceof Error && !error.name.includes('ApiError')) {
                if (error.message.includes('timeout')) {
                    throw new ApiError(
                        `Request timeout after ${duration}ms`,
                        ApiErrorType.TIMEOUT_ERROR,
                        undefined,
                        undefined,
                        correlationId
                    );
                } else if (error.message.includes('net::') || error.message.includes('ECONNREFUSED')) {
                    throw new ApiError(
                        `Network error: ${error.message}`,
                        ApiErrorType.NETWORK_ERROR,
                        undefined,
                        undefined,
                        correlationId
                    );
                }
            }

            throw error;
        }
    }

    /**
     * Refresh authentication tokens
     */
    private async refreshTokens(): Promise<AuthTokens> {
        // Prevent multiple concurrent refresh attempts
        if (this.tokenRefreshPromise) {
            return this.tokenRefreshPromise;
        }

        if (!this.authTokens?.refreshToken) {
            throw new ApiError(
                'No refresh token available',
                ApiErrorType.AUTHENTICATION_ERROR
            );
        }

        this.tokenRefreshPromise = this.performTokenRefresh();

        try {
            const newTokens = await this.tokenRefreshPromise;
            this.authTokens = newTokens;
            return newTokens;
        } finally {
            this.tokenRefreshPromise = null;
        }
    }

    /**
     * Perform the actual token refresh request
     */
    private async performTokenRefresh(): Promise<AuthTokens> {
        if (!this.context || !this.authTokens?.refreshToken) {
            throw new ApiError(
                'Cannot refresh tokens: missing context or refresh token',
                ApiErrorType.AUTHENTICATION_ERROR
            );
        }

        const startTime = Date.now();

        try {
            const headers: Record<string, string> = {
                'Content-Type': 'application/json'
            };

            if (this.tenantId) {
                headers['X-Tenant-ID'] = this.tenantId;
            }

            const response = await this.context.post('/api/v1/auth/refresh', {
                headers,
                data: {
                    refreshToken: this.authTokens.refreshToken
                }
            });

            const duration = Date.now() - startTime;
            const responseData = await response.json();

            if (!response.ok()) {
                throw this.createApiError(response.status(), responseData, duration);
            }

            const tokenResponse: TokenRefreshResponse = responseData;
            const newTokens: AuthTokens = {
                accessToken: tokenResponse.accessToken,
                refreshToken: tokenResponse.refreshToken,
                tokenType: tokenResponse.tokenType,
                expiresIn: tokenResponse.expiresIn,
                refreshExpiresIn: tokenResponse.refreshExpiresIn
            };

            this.logger.info('Token refresh successful', { duration });
            return newTokens;

        } catch (error) {
            const duration = Date.now() - startTime;
            this.logger.error('Token refresh failed', {
                error: error instanceof Error ? error.message : String(error),
                duration
            });
            throw error;
        }
    }

    /**
     * Create an ApiError from response data
     */
    private createApiError(
        status: number,
        responseData: any,
        duration: number,
        correlationId?: string
    ): ApiError {
        let errorType: ApiErrorType;
        let message: string;

        // Determine error type based on status code
        if (status === 401) {
            errorType = ApiErrorType.AUTHENTICATION_ERROR;
            message = 'Authentication failed';
        } else if (status === 403) {
            errorType = ApiErrorType.AUTHORIZATION_ERROR;
            message = 'Authorization failed';
        } else if (status === 404) {
            errorType = ApiErrorType.NOT_FOUND_ERROR;
            message = 'Resource not found';
        } else if (status === 400) {
            errorType = ApiErrorType.VALIDATION_ERROR;
            message = 'Validation failed';
        } else if (status === 429) {
            errorType = ApiErrorType.RATE_LIMIT_ERROR;
            message = 'Rate limit exceeded';
        } else if (status >= 500) {
            errorType = ApiErrorType.SERVER_ERROR;
            message = 'Server error';
        } else {
            errorType = ApiErrorType.UNKNOWN_ERROR;
            message = 'Unknown error';
        }

        // Extract error message from response if available
        if (responseData && typeof responseData === 'object') {
            if (responseData.error?.message) {
                message = responseData.error.message;
            } else if (responseData.message) {
                message = responseData.message;
            }
        }

        const apiResponse: ApiResponse = {
            status,
            headers: {},
            data: responseData,
            duration,
            ...(correlationId && { correlationId })
        };

        return new ApiError(
            message,
            errorType,
            status,
            apiResponse,
            correlationId,
            responseData?.error?.requestId
        );
    }

    /**
     * Generate a unique correlation ID
     */
    private generateCorrelationId(): string {
        return `test-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    }

    /**
     * Sleep for specified milliseconds
     */
    private sleep(ms: number): Promise<void> {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    /**
     * Get current authentication tokens
     */
    getAuthTokens(): AuthTokens | null {
        return this.authTokens;
    }

    /**
     * Get current tenant ID
     */
    getTenantId(): string | null {
        return this.tenantId;
    }

    /**
     * Check if client is authenticated
     */
    isAuthenticated(): boolean {
        return !!this.authTokens?.accessToken;
    }

    /**
     * Clear authentication tokens
     */
    clearAuth(): void {
        this.authTokens = null;
        this.logger.debug('Authentication tokens cleared');
    }

    /**
     * Dispose of the API client and clean up resources
     */
    async dispose(): Promise<void> {
        if (this.context) {
            await this.context.dispose();
            this.context = null;
        }
        this.authTokens = null;
        this.tenantId = null;
        this.tokenRefreshPromise = null;

        this.logger.info('API client disposed');
    }
}

/**
 * Factory function to create an ApiClient from environment configuration
 */
export function createApiClient(envConfig: TestEnvironmentConfig, tenantId?: string): ApiClient {
    const config: ApiClientConfig = {
        baseUrl: envConfig.services.gateway,
        timeout: envConfig.timeouts.request,
        retries: envConfig.retries.networkError,
        debug: envConfig.debug,
        ...(tenantId && { tenantId })
    };

    return new ApiClient(config);
}