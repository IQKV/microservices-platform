---
name: review-code
description: Comprehensive code review using java-pro agent for Spring Boot applications
---

# Code Review Command

Use the **java-pro** subagent to perform a comprehensive code review of Java Spring Boot applications.

## Review Scope

### 1. Code Quality Assessment

- **Architecture Patterns**: Verify adherence to Spring Boot best practices and SOLID principles
- **Modern Java Features**: Ensure proper use of Java 21+ features (records, pattern matching, virtual threads)
- **Spring Conventions**: Validate proper use of annotations, dependency injection, and configuration
- **Code Style**: Check naming conventions, method length, class organization

### 2. Performance Analysis

- **Virtual Thread Usage**: Verify optimal use of virtual threads for I/O operations
- **Database Queries**: Check for N+1 queries, proper indexing, and query optimization
- **Caching Strategy**: Validate caching implementation and cache eviction policies
- **Memory Management**: Identify potential memory leaks and optimization opportunities

### 3. Security Review

- **Input Validation**: Ensure proper validation using Bean Validation annotations
- **Authentication/Authorization**: Verify Spring Security configuration and method-level security
- **SQL Injection Prevention**: Check for parameterized queries and prepared statements
- **CORS Configuration**: Validate cross-origin resource sharing settings

### 4. Testing Coverage

- **Unit Tests**: Verify comprehensive unit test coverage with JUnit 5 and Mockito
- **Integration Tests**: Check for proper integration testing with @SpringBootTest and Testcontainers
- **Test Organization**: Validate test structure and naming conventions
- **Mock Usage**: Ensure appropriate mocking strategies

### 5. Configuration Management

- **Application Properties**: Review application.yml/properties configuration
- **Profile Management**: Validate environment-specific configurations
- **Externalized Configuration**: Check for proper use of @ConfigurationProperties
- **Docker Configuration**: Review Dockerfile and docker-compose.yml optimization

## Execution Instructions

1. **Compare Changes**: Examine all changes against the default development branch
2. **Verify Commits**: Ensure every commit follows conventional commit standards
3. **Check Dependencies**: Validate Maven/Gradle dependencies and versions
4. **Review Documentation**: Assess code documentation and API documentation
5. **Generate Report**: Provide detailed findings with recommendations

## Output Format

```markdown
# Code Review Report

## Summary

- **Overall Rating**: [Excellent/Good/Needs Improvement/Poor]
- **Critical Issues**: [Count]
- **Recommendations**: [Count]

## Detailed Findings

### Architecture & Design

- [Findings and recommendations]

### Performance

- [Performance analysis results]

### Security

- [Security assessment]

### Testing

- [Test coverage and quality]

### Configuration

- [Configuration review]

## Action Items

1. [Priority 1 items]
2. [Priority 2 items]
3. [Priority 3 items]

## Recommendations

- [Specific improvement suggestions]
```
