/** @type {import('jest').Config} */
module.exports = {
  preset: 'jest-preset-angular',
  setupFilesAfterEnv: ['<rootDir>/setup-jest.ts'],
  testEnvironment: 'jsdom',

  roots: ['<rootDir>/src'],
  testMatch: ['**/*.spec.ts'],
  testPathIgnorePatterns: ['<rootDir>/node_modules/', '<rootDir>/dist/'],

  // Resuelve los imports absolutos "src/..." y los aliases "@core/...", "@shared/...", etc.
  moduleNameMapper: {
    '^@core/(.*)$': '<rootDir>/src/app/core/$1',
    '^@shared/(.*)$': '<rootDir>/src/app/shared/$1',
    '^@features/(.*)$': '<rootDir>/src/app/features/$1',
    '^@ui/(.*)$': '<rootDir>/src/app/ui/$1',
    '^@env/(.*)$': '<rootDir>/src/environments/$1',
    '^src/(.*)$': '<rootDir>/src/$1',
  },

  coverageDirectory: '<rootDir>/coverage',
  coverageReporters: ['html', 'text-summary', 'lcovonly'],
  collectCoverageFrom: [
    'src/app/**/*.ts',
    '!src/app/**/*.spec.ts',
    '!src/app/**/*.routes.ts',
    '!src/app/**/ui/**',
    '!src/**/index.ts',
  ],
};
