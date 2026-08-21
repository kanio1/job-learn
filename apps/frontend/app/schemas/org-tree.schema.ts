import { z } from 'zod'

export const orgTreeNodeSchema = z.object({
  id: z.string().min(1),
  type: z.enum(['TENANT', 'MERCHANT']),
  label: z.string(),
  reference: z.string(),
  lazy: z.boolean(),
})

export const orgTreeResponseSchema = z.object({
  nodes: z.array(orgTreeNodeSchema),
})

export type OrgTreeNode = z.infer<typeof orgTreeNodeSchema>
export type OrgTreeResponse = z.infer<typeof orgTreeResponseSchema>
