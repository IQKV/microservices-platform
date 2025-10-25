/**
 * JWT Token Testing Utilities
 * Provides utilities for testing JWT token lifecycle and validation
 */

import { AuthTokens, UserContext } from '../types/api-responses.js';

export interface JwtPayload {
  sub: string; // Subject (user ID)
  username: string;
  email: string;
  roles: string[];
  permissions: string[];
  tenantId?: string;
  iat: number; // Issued at
  exp: number; // Expiration time
  iss: string; // Issuer
  aud: string; // Audience
  jti: string; // JWT ID
}

export interface TokenValidationResult {
  isValid: boolean;
  isExpired: boolean;
  payload?: JwtPayload;
  error?: string;
  expiresIn?: number; // Seconds until expiration
}

export interface TokenComparisonResult {
  accessTokenChanged: boolean;
  refreshTokenChanged: boolean;
  expirationExtended: boolean;
  payloadDifferences: string[];
}

/**
 * JWT Token Testing Utilities
 */
export class JwtTestUtils {
  
  /**
   * Decode JWT token without verification (for testing purposes)
   */
  static decodeToken(token: string): JwtPayload | null {
    try {
      const parts = token.split('.');
      if (parts.length !== 3) {
        return null;
      }

      const payload = parts[1];
      const decoded = Buffer.from(payload, 'base64url').toString('utf8');
      return JSON.parse(decoded) as JwtPayload;
    } catch {
      return null;
    }
  }

  /**
   * Validate JWT token structure and expiration
   */
  static validateToken(token: string): TokenValidationResult {
    try {
      const payload = this.decodeToken(token);
      
      if (!payload) {
        return {
          isValid: false,
          isExpired: false,
          error: 'Invalid token format'
        };
      }

      const now = Math.floor(Date.now() / 1000);
      const isExpired = payload.exp <= now;
      const expiresIn = Math.max(0, payload.exp - now);

      return {
        isValid: true,
        isExpired,
        payload,
        expiresIn
      };
    } catch (error) {
      return {
        isValid: false,
        isExpired: false,
        error: error instanceof Error ? error.message : 'Unknown error'
      };
    }
  }

  /**
   * Extract user context from JWT token
   */
  static extractUserContext(token: string): UserContext | null {
    const payload = this.decodeToken(token);
    if (!payload) {
      return null;
    }

    return {
      userId: parseInt(payload.sub, 10),
      username: payload.username,
      email: payload.email,
      roles: payload.roles || [],
      permissions: payload.permissions || [],
      tenantId: payload.tenantId
    };
  }

  /**
   * Check if token has specific role
   */
  static hasRole(token: string, role: string): boolean {
    const payload = this.decodeToken(token);
    return payload?.roles?.includes(role) || false;
  }

  /**
   * Check if token has specific permission
   */
  static hasPermission(token: string, permission: string): boolean {
    const payload = this.decodeToken(token);
    return payload?.permissions?.includes(permission) || false;
  }

  /**
   * Check if token belongs to specific tenant
   */
  static belongsToTenant(token: string, tenantId: string): boolean {
    const payload = this.decodeToken(token);
    return payload?.tenantId === tenantId;
  }

  /**
   * Compare two sets of tokens to detect changes
   */
  static compareTokens(oldTokens: AuthTokens, newTokens: AuthTokens): TokenComparisonResult {
    const oldPayload = this.decodeToken(oldTokens.accessToken);
    const newPayload = this.decodeToken(newTokens.accessToken);

    const payloadDifferences: string[] = [];

    if (oldPayload && newPayload) {
      // Check for payload differences
      if (oldPayload.exp !== newPayload.exp) {
        payloadDifferences.push('expiration time changed');
      }
      if (oldPayload.iat !== newPayload.iat) {
        payloadDifferences.push('issued at time changed');
      }
      if (JSON.stringify(oldPayload.roles) !== JSON.stringify(newPayload.roles)) {
        payloadDifferences.push('roles changed');
      }
      if (JSON.stringify(oldPayload.permissions) !== JSON.stringify(newPayload.permissions)) {
        payloadDifferences.push('permissions changed');
      }
      if (oldPayload.tenantId !== newPayload.tenantId) {
        payloadDifferences.push('tenant ID changed');
      }
    }

    return {
      accessTokenChanged: oldTokens.accessToken !== newTokens.accessToken,
      refreshTokenChanged: oldTokens.refreshToken !== newTokens.refreshToken,
      expirationExtended: oldPayload && newPayload ? newPayload.exp > oldPayload.exp : false,
      payloadDifferences
    };
  }

  /**
   * Calculate time until token expiration
   */
  static getTimeUntilExpiration(token: string): number {
    const payload = this.decodeToken(token);
    if (!payload) {
      return 0;
    }

    const now = Math.floor(Date.now() / 1000);
    return Math.max(0, payload.exp - now);
  }

  /**
   * Check if token will expire within specified seconds
   */
  static willExpireSoon(token: string, withinSeconds: number): boolean {
    const timeUntilExpiration = this.getTimeUntilExpiration(token);
    return timeUntilExpiration <= withinSeconds;
  }

  /**
   * Generate mock JWT token for testing (not cryptographically signed)
   */
  static generateMockToken(payload: Partial<JwtPayload>): string {
    const header = {
      alg: 'HS256',
      typ: 'JWT'
    };

    const now = Math.floor(Date.now() / 1000);
    const fullPayload: JwtPayload = {
      sub: '1',
      username: 'testuser',
      email: 'test@example.com',
      roles: ['USER'],
      permissions: ['READ'],
      iat: now,
      exp: now + 3600, // 1 hour
      iss: 'gripday-auth-service',
      aud: 'gripday-platform',
      jti: 'test-jwt-id',
      ...payload
    };

    const encodedHeader = Buffer.from(JSON.stringify(header)).toString('base64url');
    const encodedPayload = Buffer.from(JSON.stringify(fullPayload)).toString('base64url');
    const signature = 'mock-signature'; // Not a real signature

    return `${encodedHeader}.${encodedPayload}.${signature}`;
  }

  /**
   * Generate expired mock token for testing
   */
  static generateExpiredMockToken(payload: Partial<JwtPayload> = {}): string {
    const now = Math.floor(Date.now() / 1000);
    return this.generateMockToken({
      ...payload,
      iat: now - 7200, // 2 hours ago
      exp: now - 3600  // 1 hour ago (expired)
    });
  }

  /**
   * Generate mock tokens with specific roles
   */
  static generateMockTokensWithRoles(roles: string[]): AuthTokens {
    const now = Math.floor(Date.now() / 1000);
    
    const accessToken = this.generateMockToken({
      roles,
      permissions: this.getPermissionsForRoles(roles),
      exp: now + 900 // 15 minutes
    });

    const refreshToken = this.generateMockToken({
      roles,
      permissions: this.getPermissionsForRoles(roles),
      exp: now + 86400 // 24 hours
    });

    return {
      accessToken,
      refreshToken,
      tokenType: 'Bearer',
      expiresIn: 900,
      refreshExpiresIn: 86400
    };
  }

  /**
   * Get typical permissions for roles (for testing)
   */
  private static getPermissionsForRoles(roles: string[]): string[] {
    const permissions: string[] = [];
    
    if (roles.includes('USER')) {
      permissions.push('READ', 'WRITE_OWN');
    }
    
    if (roles.includes('ADMIN')) {
      permissions.push('READ', 'WRITE', 'DELETE', 'ADMIN');
    }
    
    if (roles.includes('MODERATOR')) {
      permissions.push('READ', 'WRITE', 'MODERATE');
    }

    return [...new Set(permissions)]; // Remove duplicates
  }

  /**
   * Validate token format without decoding
   */
  static isValidTokenFormat(token: string): boolean {
    if (!token || typeof token !== 'string') {
      return false;
    }

    const parts = token.split('.');
    return parts.length === 3 && parts.every(part => part.length > 0);
  }

  /**
   * Extract token from Authorization header
   */
  static extractTokenFromHeader(authHeader: string): string | null {
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return null;
    }

    return authHeader.substring(7); // Remove 'Bearer ' prefix
  }

  /**
   * Create Authorization header from token
   */
  static createAuthHeader(token: string): string {
    return `Bearer ${token}`;
  }

  /**
   * Validate tokens object structure
   */
  static validateTokensStructure(tokens: any): tokens is AuthTokens {
    return (
      tokens &&
      typeof tokens === 'object' &&
      typeof tokens.accessToken === 'string' &&
      typeof tokens.refreshToken === 'string' &&
      typeof tokens.tokenType === 'string' &&
      typeof tokens.expiresIn === 'number' &&
      typeof tokens.refreshExpiresIn === 'number' &&
      this.isValidTokenFormat(tokens.accessToken) &&
      this.isValidTokenFormat(tokens.refreshToken)
    );
  }

  /**
   * Get token claims for debugging
   */
  static getTokenClaims(token: string): Record<string, any> | null {
    const payload = this.decodeToken(token);
    if (!payload) {
      return null;
    }

    return {
      subject: payload.sub,
      username: payload.username,
      email: payload.email,
      roles: payload.roles,
      permissions: payload.permissions,
      tenantId: payload.tenantId,
      issuedAt: new Date(payload.iat * 1000).toISOString(),
      expiresAt: new Date(payload.exp * 1000).toISOString(),
      issuer: payload.iss,
      audience: payload.aud,
      jwtId: payload.jti
    };
  }
}