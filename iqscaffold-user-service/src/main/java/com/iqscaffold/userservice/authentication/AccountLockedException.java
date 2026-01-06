package com.iqscaffold.userservice.authentication;

/**
 * Custom exception for account lockout scenarios.
 * 
 * <p>This exception is thrown when an account is locked due to:
 * <ul>
 *   <li>Excessive failed authentication attempts</li>
 *   <li>Security policy violations</li>
 *   <li>Administrative account suspension</li>
 * </ul>
 * 
 * @author IQScaffold Team
 * @version 1.0
 * @since 1.0
 */
public class AccountLockedException extends RuntimeException {

    public AccountLockedException(final String message) {
        super(message);
    }
}