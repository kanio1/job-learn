import type { ApiResponse } from '~/types/api'
import {
  createUserSchema,
  roleAssignmentSchema,
  updateUserSchema,
  userDetailSchema,
  userListSchema,
  type CreateUserInput,
  type RoleAssignmentInput,
  type UpdateUserInput,
  type UserDetail,
  type UserList,
  type UsersQuery,
} from '~/schemas/user.schema'

export function useUsersApi() {
  const { request } = useApiClient()

  async function listUsers(query?: UsersQuery): Promise<ApiResponse<UserList>> {
    return request('/api/users', userListSchema, {
      // SAFETY: UsersQuery is page/size/search/role/status scalars, not nested JSON.
      query: query as Record<string, string | number | boolean | null | undefined> | undefined,
    })
  }

  async function createUser(payload: CreateUserInput): Promise<ApiResponse<UserDetail>> {
    return request('/api/users', userDetailSchema, {
      method: 'POST',
      body: createUserSchema.parse(payload),
    })
  }

  async function getUser(id: string): Promise<ApiResponse<UserDetail>> {
    return request(`/api/users/${encodeURIComponent(id)}`, userDetailSchema)
  }

  async function updateUser(
    id: string,
    payload: UpdateUserInput
  ): Promise<ApiResponse<UserDetail>> {
    return request(`/api/users/${encodeURIComponent(id)}`, userDetailSchema, {
      method: 'PATCH',
      body: updateUserSchema.parse(payload),
    })
  }

  async function assignUserRoles(
    id: string,
    payload: RoleAssignmentInput
  ): Promise<ApiResponse<UserDetail>> {
    return request(`/api/users/${encodeURIComponent(id)}/roles`, userDetailSchema, {
      method: 'POST',
      body: roleAssignmentSchema.parse(payload),
    })
  }

  return { listUsers, createUser, getUser, updateUser, assignUserRoles }
}

export type {
  CreateUserInput,
  RoleAssignmentInput,
  UpdateUserInput,
  UserDetail,
  UserList,
  UsersQuery,
}
