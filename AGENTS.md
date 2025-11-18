# Repository Guidelines & Agent Instructions

## Overview

This document provides comprehensive guidelines for repository management, development workflows, and collaboration standards for multi-module Spring Boot projects. It serves as a reference
for both human developers and AI agents working with this codebase.

## 🏛️ Repository Structure & Organization

### Multi-Module Maven Layout

```
multimoduleservice/
├── .github/                   # GitHub workflows, templates, and policies
│   ├── workflows/            # CI/CD pipeline definitions
│   ├── ISSUE_TEMPLATE/       # Issue templates for different types
│   └── pull_request_template.md
├── .claude/                  # Claude AI agent configurations
│   ├── agents/              # Specialized agent definitions
│   └── commands/            # Reusable command templates
├── shared/                   # Common utilities, DTOs, interfaces
├── domain/                   # Domain models, business logic
├── infrastructure/           # External integrations, repositories
├── api/                     # REST controllers, web layer
├── application/             # Application services, orchestration
├── docs/                    # Documentation and architectural decisions
├── scripts/                 # Build, deployment, and utility scripts
├── docker/                  # Docker configurations and compose files
└── k8s/                     # Kubernetes manifests
```

### Module Dependencies

```mxml
<!-- Parent POM manages versions -->
<parent>
    <groupId>com.iqkv</groupId>
    <artifactId>boot-parent-pom</artifactId>
    <version>0.25.0-SNAPSHOT</version>
</parent>

<!-- Module dependency hierarchy -->
api → application → domain ← infrastructure
     ↘ shared ↙
```

## 🤖 AI Agent Guidelines

### Agent Roles & Responsibilities

#### 1. Java Pro Agent (`java-pro.md`)

**Purpose**: Expert Java 21+ development with Spring Boot 3.x and modern JVM features
**Responsibilities**:

- Modern Java feature implementation (virtual threads, pattern matching, records)
- Spring Boot 3.x architecture and best practices
- Performance optimization and JVM tuning
- Code generation and refactoring with modern patterns
- Testing strategy development and implementation
- Cloud-native application development

**Activation Triggers**:

- `*.java`, `pom.xml`, `application.yml`, `Dockerfile` file modifications
- Performance optimization requests
- Architecture reviews and modernization
- Testing implementation and coverage improvement
- Spring Boot upgrades and migration

#### 2. Spring Security Expert (`spring-security-expert.md`)

**Purpose**: Specialized Spring Security 6+ authentication and authorization expert
**Responsibilities**:

- OAuth2, OIDC, and JWT implementation
- Method-level security and custom security expressions
- Enterprise authentication integration (LDAP, SAML)
- Security configuration and best practices
- Audit logging and compliance requirements

**Activation Triggers**:

- `*Security*.java`, `*Auth*.java` file modifications
- Security configuration changes
- Authentication and authorization implementation
- Security vulnerability assessments

#### 3. Performance Optimizer (`performance-optimizer.md`)

**Purpose**: JVM performance tuning and application optimization specialist
**Responsibilities**:

- Virtual thread implementation and optimization
- JVM tuning (GC, memory management, native compilation)
- Caching strategies and database optimization
- Performance monitoring and profiling
- Load testing and benchmarking

**Activation Triggers**:

- Performance-related keywords (slow, memory, cpu, cache)
- Performance testing and optimization requests
- JVM tuning and configuration
- Monitoring and observability setup

#### 2. Repository Manager Agent

**Purpose**: Repository maintenance and workflow management
**Responsibilities**:

- Branch management strategies
- PR review guidelines
- Release management
- Documentation updates
- Dependency management

#### 3. Security Agent

**Purpose**: Security best practices and vulnerability management
**Responsibilities**:

- Security code reviews
- Vulnerability scanning
- Authentication/authorization implementation
- Secure configuration management
- Compliance validation

#### 4. DevOps Agent

**Purpose**: CI/CD pipeline and deployment automation
**Responsibilities**:

- Pipeline configuration
- Container orchestration
- Environment management
- Monitoring setup
- Infrastructure as code

### Agent Interaction Protocols

#### Code Review Process

```yaml
agent_review_workflow:
  triggers:
    - pull_request_opened
    - pull_request_synchronized

  review_checklist:
    - code_quality: "Java Pro Agent validates code standards"
    - security: "Security Agent checks for vulnerabilities"
    - performance: "Performance impact analysis"
    - testing: "Test coverage and quality validation"
    - documentation: "Documentation completeness check"
```

#### Automated Tasks

```yaml
automated_tasks:
  daily:
    - dependency_updates: "Check for security updates and CVE scanning"
    - code_quality_metrics: "Generate quality reports with SonarQube"
    - performance_benchmarks: "Run JMH benchmarks and load tests"
    - virtual_thread_analysis: "Monitor virtual thread performance"

  weekly:
    - architecture_review: "Assess architectural debt and modernization opportunities"
    - security_scan: "Comprehensive security analysis with OWASP tools"
    - documentation_audit: "Update documentation gaps and API docs"
    - jvm_optimization: "Analyze GC logs and memory usage patterns"

  monthly:
    - spring_boot_updates: "Evaluate Spring Boot version upgrades"
    - java_feature_adoption: "Assess new Java feature adoption opportunities"
    - performance_baseline: "Establish performance baselines and trends"
```

## 📋 Development Standards

### Branch Strategy (GitFlow)

```
main (production)
├── develop (integration)
│   ├── feature/TICKET-123-description
│   ├── feature/add-user-management
│   └── feature/implement-caching
├── release/v1.2.0
├── hotfix/critical-bug-fix
└── support/v1.1.x (LTS support)
```

### Branch Naming Conventions

```bash
# Feature branches
feature/TICKET-123-short-description
feature/add-payment-processing
feature/implement-oauth2

# Bug fixes
bugfix/TICKET-456-fix-null-pointer
bugfix/resolve-memory-leak

# Hotfixes
hotfix/critical-security-patch
hotfix/production-data-fix

# Release branches
release/v1.2.0
release/v2.0.0-beta

# Support branches
support/v1.1.x
```

### Commit Message Format

```
type(scope): subject

body (optional)

footer (optional)

# Types
feat:     New feature
fix:      Bug fix
docs:     Documentation changes
style:    Code style changes (formatting, etc.)
refactor: Code refactoring
test:     Test additions or modifications
chore:    Build process or auxiliary tool changes
perf:     Performance improvements
ci:       CI/CD pipeline changes

# Examples
feat(api): add user authentication endpoint

fix(database): resolve connection pool exhaustion
- Increase maximum pool size to 20
- Add connection timeout configuration
- Update health check query

Closes #123
```

### Code Quality Standards

#### Java Code Standards

```java
// Use modern Java features
public record UserCreateRequest(@NotBlank @Size(max = 100) String username, @Email String email, @Valid AddressDto address) {}

// Prefer immutable objects
@Value
@Builder
public class User {

  String id;
  String username;
  String email;
  Instant createdAt;
  List<String> roles;
}

// Use descriptive method names
public Optional<User> findActiveUserByEmail(String email) {
  return userRepository.findByEmailAndActiveTrue(email);
}

// Document complex business logic
/**
 * Calculates user subscription cost based on plan and usage.
 *
 * @param user the user requesting calculation
 * @param plan the subscription plan
 * @param usageMetrics current usage data
 * @return calculated cost with tax included
 * @throws InvalidPlanException if plan is not valid for user
 */
public Money calculateSubscriptionCost(User user, Plan plan, UsageMetrics usageMetrics) {
  // Implementation
}
```

#### Testing Standards

```java
// Unit Tests - AAA Pattern
@Test
@DisplayName("Should calculate subscription cost for premium plan")
void shouldCalculateSubscriptionCostForPremiumPlan() {
  // Arrange
  var user = createTestUser();
  var plan = Plan.PREMIUM;
  var usageMetrics = UsageMetrics.builder().apiCalls(1000).storageGB(50).build();

  // Act
  var cost = subscriptionService.calculateSubscriptionCost(user, plan, usageMetrics);

  // Assert
  assertThat(cost.getAmount()).isEqualByComparingTo("99.99");
  assertThat(cost.getCurrency()).isEqualTo("USD");
}

// Integration Tests with Testcontainers
@SpringBootTest
@Testcontainers
class UserServiceIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

  @Test
  void shouldCreateUserWithValidData() {
    // Test implementation
  }
}

// Architecture Tests
@AnalyzeClasses(packages = "com.iqkv")
class ArchitectureTest {

  @ArchTest
  static final ArchRule servicesOnlyAccessedByControllers = classes().that().resideInAPackage("..service..").should().onlyBeAccessed().byAnyPackage("..controller..", "..service..");
}
```

## 🔄 Workflow Management

### Pull Request Guidelines

#### PR Template

```markdown
## Description

Brief description of changes and motivation.

## Type of Change

- [ ] Bug fix (non-breaking change that fixes an issue)
- [ ] New feature (non-breaking change that adds functionality)
- [ ] Breaking change (fix or feature that causes existing functionality to change)
- [ ] Documentation update
- [ ] Performance improvement
- [ ] Refactoring

## How Has This Been Tested?

- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing performed
- [ ] Performance testing completed

## Checklist

- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Code is documented (complex logic)
- [ ] Tests cover new/modified code
- [ ] All tests pass locally
- [ ] No new warnings or errors
- [ ] Breaking changes documented

## Screenshots (if applicable)

## Additional Notes
```

#### Review Criteria

```yaml
review_requirements:
  mandatory_reviews: 2
  required_reviewers:
    - code_owner: "Module maintainer approval"
    - security_review: "For security-related changes"
    - architecture_review: "For significant architectural changes"

  blocking_conditions:
    - failing_tests: "All tests must pass"
    - merge_conflicts: "Must be up to date with target branch"
    - missing_documentation: "Public APIs must be documented"
    - security_vulnerabilities: "No known security issues"
    - performance_regression: "No significant performance degradation"
```

### Release Management

#### Versioning Strategy (Semantic Versioning)

```
MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]

Examples:
1.0.0        # Initial release
1.1.0        # Minor feature addition
1.1.1        # Bug fix patch
2.0.0        # Breaking changes
2.0.0-beta.1 # Beta release
2.0.0-rc.1   # Release candidate
```

#### Release Process

```yaml
release_workflow:
  preparation:
    - create_release_branch: "release/vX.Y.Z"
    - update_version_numbers: "Maven versions update"
    - run_full_test_suite: "All tests must pass"
    - generate_changelog: "Document all changes"
    - security_scan: "Vulnerability assessment"

  release:
    - merge_to_main: "Create release commit"
    - create_git_tag: "Tag with version number"
    - build_artifacts: "Create deployable artifacts"
    - deploy_to_staging: "Staging environment validation"
    - deploy_to_production: "Production deployment"

  post_release:
    - merge_back_to_develop: "Include any release fixes"
    - update_documentation: "Release notes and docs"
    - notify_stakeholders: "Communication plan"
```

## 🔒 Security Guidelines

### Security Checklist

```yaml
security_requirements:
  code_review:
    - input_validation: "All user inputs validated"
    - output_encoding: "XSS prevention measures"
    - authentication: "Proper authentication checks"
    - authorization: "Access control validation"
    - data_protection: "Sensitive data handling"

  dependencies:
    - vulnerability_scanning: "Regular dependency scans"
    - license_compliance: "License compatibility check"
    - update_policy: "Security update procedures"

  infrastructure:
    - secrets_management: "No hardcoded secrets"
    - network_security: "Secure communication protocols"
    - logging_security: "No sensitive data in logs"
```

### Secrets Management

```bash
# Environment variables for secrets
export DATABASE_PASSWORD="${DB_PASSWORD}"
export JWT_SECRET="${JWT_SECRET_KEY}"
export API_KEY="${EXTERNAL_API_KEY}"

# Never commit secrets to repository
# Use .env files for local development (gitignored)
# Use secret management systems for production
```

## 📊 Quality Assurance

### Code Quality Metrics

```yaml
quality_gates:
  coverage:
    line_coverage: ">= 90%"
    branch_coverage: ">= 85%"
    mutation_coverage: ">= 80%"

  complexity:
    cyclomatic_complexity: "<= 10"
    cognitive_complexity: "<= 15"
    class_size: "<= 500 lines"
    method_length: "<= 50 lines"

  maintainability:
    duplication: "<= 3%"
    technical_debt_ratio: "<= 5%"
    maintainability_rating: "A"

  reliability:
    bug_density: "<= 0.1%"
    vulnerability_density: "0"
    reliability_rating: "A"
```

### Automated Quality Checks

```xml
<!-- Maven configuration for quality gates -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>INSTRUCTION</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.90</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>

<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <configuration>
        <effort>Max</effort>
        <threshold>Low</threshold>
        <failOnError>true</failOnError>
    </configuration>
</plugin>
```

## 🚀 CI/CD Pipeline

### GitHub Actions Workflow

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: "21"
          distribution: "temurin"

      - name: Cache Maven dependencies
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}

      - name: Run tests
        run: ./mvnw clean verify

      - name: Upload coverage reports
        uses: codecov/codecov-action@v3

      - name: Security scan
        run: ./mvnw org.owasp:dependency-check-maven:check

  build:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - name: Build Docker image
        run: docker build -t app:${{ github.sha }} .

      - name: Push to registry
        run: docker push app:${{ github.sha }}

  deploy:
    needs: build
    runs-on: ubuntu-latest
    environment: production
    if: github.ref == 'refs/heads/main'
    steps:
      - name: Deploy to production
        run: echo "Deploying to production"
```

### Environment Management

```yaml
environments:
  local:
    description: "Developer workstation"
    database: "H2 in-memory"
    external_services: "Mocked"
    logging: "DEBUG level"

  development:
    description: "Shared development environment"
    database: "PostgreSQL (dev instance)"
    external_services: "Development APIs"
    logging: "INFO level"

  staging:
    description: "Production-like testing"
    database: "PostgreSQL (staging)"
    external_services: "Production APIs (test mode)"
    logging: "WARN level"

  production:
    description: "Live production environment"
    database: "PostgreSQL (production cluster)"
    external_services: "Production APIs"
    logging: "ERROR level"
```

## 📚 Documentation Standards

### README Structure

```markdown
# Project Title

## Overview

Brief description of the project and its purpose.

## Quick Start

Instructions to get the project running locally.

## Architecture

High-level architecture overview and design decisions.

## Development

Development setup, coding standards, and contribution guidelines.

## Deployment

Deployment procedures and environment configurations.

## API Documentation

Link to API documentation (Swagger/OpenAPI).

## Monitoring

Observability and monitoring setup.

## Troubleshooting

Common issues and solutions.

## Contributing

How to contribute to the project.

## License

License information.
```

### API Documentation

```java
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "User CRUD operations")
public class UserController {

  @GetMapping("/{id}")
  @Operation(
    summary = "Get user by ID",
    description = "Retrieves a user by their unique identifier",
    responses = {
      @ApiResponse(responseCode = "200", description = "User found"),
      @ApiResponse(responseCode = "404", description = "User not found"),
      @ApiResponse(responseCode = "403", description = "Access denied"),
    }
  )
  public ResponseEntity<UserResponse> getUser(@Parameter(description = "User ID", example = "123") @PathVariable Long id) {
    // Implementation
  }
}
```

## 🎯 Agent Decision Framework

### When to Intervene

```yaml
intervention_triggers:
  high_priority:
    - security_vulnerability: "Immediate action required"
    - build_failure: "Blocks development progress"
    - production_issue: "Service disruption"
    - data_corruption: "Risk of data loss"

  medium_priority:
    - code_quality_degradation: "Technical debt accumulation"
    - performance_regression: "User experience impact"
    - test_failure: "Reliability concerns"
    - dependency_update: "Security or feature updates"

  low_priority:
    - documentation_gap: "Improved maintainability"
    - code_optimization: "Performance improvement"
    - refactoring_opportunity: "Code quality enhancement"
```

### Decision Matrix

```yaml
decision_criteria:
  impact_assessment:
    - user_impact: "How many users affected?"
    - business_impact: "Revenue or operation impact?"
    - technical_impact: "System stability and performance?"

  effort_estimation:
    - development_effort: "Time required for implementation"
    - testing_effort: "Validation and quality assurance"
    - deployment_effort: "Release and rollout complexity"

  risk_evaluation:
    - implementation_risk: "Likelihood of issues during development"
    - deployment_risk: "Risk of production problems"
    - rollback_capability: "Ability to revert changes"
```

This repository guidelines document serves as a comprehensive reference for maintaining high-quality, secure, and well-organized codebases while facilitating effective collaboration between
human developers and AI agents.
