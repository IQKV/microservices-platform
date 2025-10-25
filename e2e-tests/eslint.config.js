import eslint from '@eslint/js';
import tseslint from '@typescript-eslint/eslint-plugin';
import tsparser from '@typescript-eslint/parser';

export default [
  eslint.configs.recommended,
  {
    files: ['**/*.ts'],
    languageOptions: {
      parser: tsparser,
      parserOptions: {
        ecmaVersion: 2022,
        sourceType: 'module',
        project: './tsconfig.json'
      }
    },
    plugins: {
      '@typescript-eslint': tseslint
    },
    rules: {
      // TypeScript specific rules
      '@typescript-eslint/no-unused-vars': 'error',
      '@typescript-eslint/no-explicit-any': 'warn',
      '@typescript-eslint/explicit-function-return-type': 'off',
      '@typescript-eslint/explicit-module-boundary-types': 'off',
      '@typescript-eslint/no-non-null-assertion': 'warn',
      
      // General rules
      'no-console': 'off', // Allow console.log in tests
      'prefer-const': 'error',
      'no-var': 'error',
      'object-shorthand': 'error',
      'prefer-template': 'error',
      
      // Test-specific rules
      'max-len': ['error', { code: 120, ignoreStrings: true, ignoreTemplateLiterals: true }],
      'no-magic-numbers': 'off', // Allow magic numbers in tests
      'no-duplicate-imports': 'error'
    }
  },
  {
    files: ['**/*.spec.ts', '**/*.test.ts'],
    rules: {
      // More lenient rules for test files
      '@typescript-eslint/no-explicit-any': 'off',
      'max-len': 'off'
    }
  },
  {
    ignores: [
      'node_modules/',
      'dist/',
      'reports/',
      'test-results/',
      'playwright-report/',
      '*.config.js'
    ]
  }
];