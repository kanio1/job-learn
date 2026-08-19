import { defineConfig } from 'oxlint'

/**
 * Lab Oxlint + vendored anti-slop.
 * Policy: apps/frontend/tools/oxlint/README.md
 */
export default defineConfig({
  ignorePatterns: [
    '.nuxt/**',
    '.output/**',
    'node_modules/**',
    'dist/**',
    'playwright-report/**',
    'test-results/**',
    'coverage/**',
    'tools/oxlint/anti-slop/**',
    '**/*.png',
    '**/*.aria.yml',
  ],
  jsPlugins: [
    { name: 'anti-slop', specifier: './tools/oxlint/anti-slop/index.ts' },
  ],
  rules: {
    'anti-slop/no-chained-type-assertions': 'error',
    'anti-slop/no-widen-then-assert': 'error',
    'anti-slop/no-reflect-apply': 'error',
    'anti-slop/no-reflect-get': 'error',
    'anti-slop/no-object-parameters': 'error',
    'anti-slop/require-safety-comment-for-type-assertion': 'warn',
    'anti-slop/no-shape-in-symbol-names': 'error',
    'anti-slop/no-conditional-empty-object-spread': 'warn',
    'anti-slop/no-known-value-widening': 'warn',
    'anti-slop/no-module-mocking': 'warn',
    'anti-slop/no-runtime-typeof': ['warn', { allowInTypeGuards: true }],
    'anti-slop/no-unknown-parameters': 'warn',
    'anti-slop/no-unknown-returns': 'warn',
    'anti-slop/no-unknown-type-aliases': 'warn',
    'anti-slop/no-unsafe-dictionary-type': 'warn',
  },
  overrides: [
    {
      files: ['**/*.{test,spec}.ts', '**/*.property.test.ts'],
      rules: {
        // Vue SFC tests mock composables; live POM does not. New production seams stay unmocked.
        'anti-slop/no-module-mocking': 'off',
      },
    },
    {
      files: ['server/api/**', 'server/routes/**'],
      rules: {
        // Nitro proxies Spring JSON without a second domain model. Client Zod is the contract oracle.
        'anti-slop/no-unknown-returns': 'off',
      },
    },
  ],
})
