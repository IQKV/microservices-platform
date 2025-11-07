# Gripday E2E API Testing Suite

Comprehensive Playwright-based API testing infrastructure for the Gripday microservices platform.

## Quick Start

### Prerequisites

- Node.js 18+
- Docker and Docker Compose
- Running Gripday services (Auth, Gateway, Bookstore)

### Installation

```bash
cd e2e-tests
npm install
npm run install:browsers
```

### Environment Setup

```bash
cp .env.example .env
# Edit .env with your environment-specific settings
```

### Running Tests

**Local Environment:**

```bash
npm run test:local
```

**Staging Environment:**

```bash
npm run test:staging
```

**With Docker:**

```bash
npm run test:docker
```

**Interactive Mode:**

```bash
npm run test:ui
```

### Test Reports

```bash
npm run test:report
```

## Project Structure

```
e2e-tests/
├── src/
│   ├── config/          # Environment configurations
│   ├── fixtures/        # Test fixtures and setup
│   ├── utils/           # Utility functions and helpers
│   ├── types/           # TypeScript type definitions
│   └── tests/           # Test specifications
├── reports/             # Generated test reports
├── screenshots/         # Test failure screenshots
└── test-data/          # Static test data files
```

## Configuration

### Environment Variables

- `TEST_ENV`: Target environment (local, staging, production)
- `TEST_TIMEOUT`: Global test timeout in milliseconds
- `TEST_RETRIES`: Number of retries for failed tests
- `TEST_WORKERS`: Number of parallel workers

### Playwright Configuration

Environment-specific settings are defined in `playwright.config.ts`:

- Local: `http://localhost:8080`
- Staging: `https://api.pynity.website`
- Production: `https://api.pynity.com`

## Development

### Type Checking

```bash
npm run type-check
```

### Linting

```bash
npm run lint
```

### Adding New Tests

1. Create test files in appropriate `src/tests/` subdirectory
2. Use `.spec.ts` suffix for test files
3. Follow existing patterns for API client usage
4. Include proper cleanup in test teardown

## CI/CD Integration

The test suite generates multiple report formats:

- HTML reports for interactive viewing
- JUnit XML for CI/CD integration
- JSON results for programmatic analysis

Reports are saved to the `reports/` directory.
