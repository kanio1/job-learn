import { defineVitestConfig } from '@nuxt/test-utils/config'

export default defineVitestConfig({
  test: {
    // Default to happy-dom — fast, no Nuxt server startup.
    // Files that mount Vue components via mountSuspended declare
    // "// @vitest-environment nuxt" at their top to opt-in per-file.
    environment: 'happy-dom',
    globals: true,
    testTimeout: 30000,
    hookTimeout: 30000,
    include: [
      'app/**/*.{test,spec}.ts',
      'tests/unit/**/*.{test,spec}.ts',
    ],
    exclude: [
      'tests-pom/**',
      'node_modules/**',
      '.nuxt/**',
    ],
  },
})
