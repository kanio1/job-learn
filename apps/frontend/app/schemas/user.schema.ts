import { z } from 'zod'

export const compositeRoleSchema = z.enum([
  'PLATFORM_ADMIN',
  'TENANT_ADMIN',
  'MERCHANT_MANAGER',
  'SUPPORT_AGENT',
  'READ_ONLY_USER',
])

export const userSummarySchema = z.object({
  id: z.string().min(1),
  username: z.string(),
  email: z.email(),
  enabled: z.boolean(),
  tenantId: z.string().nullable(),
  merchantId: z.string().nullable().optional(),
  roles: z.array(compositeRoleSchema),
})

export const userDetailSchema = userSummarySchema

export const userListSchema = z.object({
  users: z.array(userSummarySchema),
  page: z.number().int().nonnegative(),
  size: z.number().int().positive(),
  totalEstimate: z.number().int().nonnegative(),
})

export const createUserSchema = z.object({
  username: z.string().trim().min(3).max(64),
  email: z.email(),
  temporaryPassword: z.string().min(8),
  tenantId: z.string().trim().min(1).optional(),
  merchantId: z.string().trim().min(1).optional(),
  roles: z.array(compositeRoleSchema).min(1),
})

export const updateUserSchema = z.object({
  email: z.email().optional(),
  enabled: z.boolean().optional(),
  attributes: z.record(z.string(), z.array(z.string())).optional(),
}).refine(
  value => value.email !== undefined
    || value.enabled !== undefined
    || value.attributes !== undefined,
  { message: 'At least one field is required' }
)

export const roleAssignmentSchema = z.object({
  assign: z.array(compositeRoleSchema),
  remove: z.array(compositeRoleSchema),
})

export type CompositeRole = z.infer<typeof compositeRoleSchema>
export type UserSummary = z.infer<typeof userSummarySchema>
export type UserDetail = z.infer<typeof userDetailSchema>
export type UserList = z.infer<typeof userListSchema>
export type CreateUserInput = z.infer<typeof createUserSchema>
export type UpdateUserInput = z.infer<typeof updateUserSchema>
export type RoleAssignmentInput = z.infer<typeof roleAssignmentSchema>

export interface UsersQuery {
  tenantId?: string
  role?: CompositeRole
  status?: 'enabled' | 'disabled'
  search?: string
  page?: number
  size?: number
}
