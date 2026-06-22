import { describe, expect, it } from 'vitest'
import * as fc from 'fast-check'
import {
  COMPOSITE_ROLES,
  rbacMatrix,
  type Capability,
  type CompositeRole,
} from '../../app/utils/rbacMatrix'

const AUDIT_READER_ROLES = new Set<CompositeRole>([
  'PLATFORM_ADMIN',
  'SUPPORT_AGENT',
  'TENANT_ADMIN',
])
const UNKNOWN_ROLE = 'UNKNOWN_ROLE' as const
const roleArbitrary = fc.constantFrom(...COMPOSITE_ROLES, UNKNOWN_ROLE)

function capabilitiesFor(role: CompositeRole | typeof UNKNOWN_ROLE): Capability | undefined {
  return (rbacMatrix as Partial<Record<string, Capability>>)[role]
}

describe('Feature: audit-log-dashboard, Property 5: audit-log capability mapping', () => {
  // Feature: audit-log-dashboard, Property 5: canViewAuditLog is a biconditional over audit-reading roles and remains distinct from canReadAudit.
  it('grants canViewAuditLog exactly to audit-reading roles over known and unknown roles', () => {
    fc.assert(
      fc.property(roleArbitrary, (role) => {
        const expected = role !== UNKNOWN_ROLE && AUDIT_READER_ROLES.has(role)
        expect(capabilitiesFor(role)?.canViewAuditLog ?? false).toBe(expected)
      }),
      { numRuns: 100 },
    )
  })

  it.each([
    ['PLATFORM_ADMIN', true],
    ['SUPPORT_AGENT', true],
    ['TENANT_ADMIN', true],
    ['MERCHANT_MANAGER', false],
    ['READ_ONLY_USER', false],
    [UNKNOWN_ROLE, false],
  ] as const)('%s maps canViewAuditLog to %s', (role, expected) => {
    expect(capabilitiesFor(role)?.canViewAuditLog ?? false).toBe(expected)
  })

  it('keeps canViewAuditLog and canReadAudit as separate capabilities', () => {
    expect(rbacMatrix.PLATFORM_ADMIN).toHaveProperty('canViewAuditLog')
    expect(rbacMatrix.PLATFORM_ADMIN).toHaveProperty('canReadAudit')
    expect(rbacMatrix.TENANT_ADMIN.canViewAuditLog).toBe(true)
    expect(rbacMatrix.TENANT_ADMIN.canReadAudit).toBe(false)
    expect(capabilitiesFor(UNKNOWN_ROLE)?.canViewAuditLog ?? false).toBe(false)
    expect(capabilitiesFor(UNKNOWN_ROLE)?.canReadAudit ?? false).toBe(false)
  })
})
