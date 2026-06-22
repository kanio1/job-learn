import { z } from '/home/suso/job-learn/apps/frontend/node_modules/.pnpm/zod@4.4.3/node_modules/zod/index.js';

const schema = z.object({
  amountMinor: z.coerce.number().int().min(1).max(100000000),
  currency: z.enum(['PLN','EUR','USD']),
  clientOrderReference: z.string().trim().min(1).max(120),
});

const result = schema.safeParse({ amountMinor: 0, currency: 'PLN', clientOrderReference: '!' });
console.log('success:', result.success);
if (!result.success) {
  console.log('error keys:', Object.keys(result.error));
  console.log('issues:', result.error.issues);
  console.log('errors:', result.error.errors);
}
