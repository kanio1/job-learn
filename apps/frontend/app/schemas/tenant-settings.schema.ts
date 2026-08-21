import { z } from 'zod'

export const refundPolicySchema = z.enum(['MANUAL', 'AUTOMATIC'])

export const paymentPolicySchema = z.object({
  autoCapture: z.boolean(),
  maxAutoCaptureMinor: z.number().int().min(0),
  riskThreshold: z.number().int().min(0).max(100),
  refundPolicy: refundPolicySchema,
}).superRefine((value, ctx) => {
  if (value.autoCapture && value.maxAutoCaptureMinor < 1) {
    ctx.addIssue({
      code: 'custom',
      path: ['maxAutoCaptureMinor'],
      message: 'maxAutoCaptureMinor is required when autoCapture is true',
    })
  }
})

export const tenantSettingsSchema = z.object({
  contactEmail: z.string().nullable().optional(),
  timezone: z.string(),
  webhookBaseUrl: z.string().nullable().optional(),
  paymentPolicy: paymentPolicySchema,
})

export type PaymentPolicy = z.infer<typeof paymentPolicySchema>
export type TenantSettings = z.infer<typeof tenantSettingsSchema>

export const DEFAULT_PAYMENT_POLICY: PaymentPolicy = {
  autoCapture: false,
  maxAutoCaptureMinor: 0,
  riskThreshold: 50,
  refundPolicy: 'MANUAL',
}
