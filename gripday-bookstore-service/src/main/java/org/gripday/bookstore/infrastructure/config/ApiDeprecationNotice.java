package org.gripday.bookstore.infrastructure.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class ApiDeprecationNotice {
    
    private static final String DEPRECATION_HEADER = "Deprecation";
    private static final String SUNSET_HEADER = "Sunset";
    private static final String WARNING_HEADER = "Warning";
    
    /**
     * Add deprecation headers to the response for deprecated API versions
     */
    public void addDeprecationHeaders(HttpServletResponse response, String version, 
                                    LocalDate deprecationDate, LocalDate sunsetDate, 
                                    String migrationInfo) {
        
        // RFC 8594 Deprecation header
        if (deprecationDate != null) {
            response.setHeader(DEPRECATION_HEADER, "true");
        }
        
        // RFC 8594 Sunset header
        if (sunsetDate != null) {
            response.setHeader(SUNSET_HEADER, sunsetDate.format(DateTimeFormatter.RFC_1123_DATE_TIME));
        }
        
        // Warning header with migration information
        var warningMessage = String.format(
            "299 - \"API version %s is deprecated. %s\"", 
            version, 
            migrationInfo != null ? migrationInfo : "Please migrate to the latest version."
        );
        response.setHeader(WARNING_HEADER, warningMessage);
    }
    
    /**
     * Check if a version is deprecated
     */
    public boolean isVersionDeprecated(String version) {
        // Currently no versions are deprecated
        // This method can be updated when versions become deprecated
        return false;
    }
    
    /**
     * Get migration information for deprecated versions
     */
    public String getMigrationInfo(String deprecatedVersion) {
        return switch (deprecatedVersion) {
            // Future deprecation cases can be added here
            default -> "Please migrate to the latest version for continued support.";
        };
    }
}