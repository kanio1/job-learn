import { describe, expect, it } from 'vitest'
import * as fc from 'fast-check'
import {
  COMPOSITE_ROLES,
  rbacMatrix,
  type CompositeRole,
} from '../../app/utils/rbacMatrix'

const ADMIN_ROLES = new Set<CompositeRole>(['PLATFORM_ADMIN', 'TENANT_ADMIN'])
const roleArbitrary = fc.constantFrom(...COMPOSITE_ROLES)

describe('Feature: user-management, Property 5: frontend capability mapping is a biconditional on admin roles', () => {
  it('maps both user-management capabilities exactly to the two admin roles', () => {
    fc.assert(
      fc.property(roleArbitrary, (role) => {
        const expected = ADMIN_ROLES.has(role)
        const capabilities = rbacMatrix[role]

        expect(capabilities.canManageUsers).toBe(expected)
        expect(capabilities.canAssignRoles).toBe(expected)
        expect(capabilities.canManageUsers).toBe(capabilities.canAssignRoles)
      }),
      { numRuns: 100 },
    )
  })

  it.each([
    ['PLATFORM_ADMIN', true],
    ['TENANT_ADMIN', true],
    ['MERCHANT_MANAGER', false],
    ['SUPPORT_AGENT', false],
    ['READ_ONLY_USER', false],
  ] as const)('%s has both capabilities set to %s', (role, expected) => {
    expect(rbacMatrix[role].canManageUsers).toBe(expected)
    expect(rbacMatrix[role].canAssignRoles).toBe(expected)
  })
})
