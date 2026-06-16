const { z } = require('/home/suso/job-learn/apps/frontend/node_modules/.pnpm/zod@4.4.3/node_modules/zod/index.cjs');
const schema = z.object({
  amountMinor: z.coerce.number().int().min(1).max(100000000),
  currency: z.enum(['PLN','EUR','USD']),
  clientOrderReference: z.string().trim().min(1).max(120),
});
const r = schema.safeParse({ amountMinor: 0, currency: 'PLN', clientOrderReference: '!' });
console.log(JSON.stringify({ success: r.success, errorKeys: r.error ? Object.keys(r.error) : null, issues: r.error?.issues, errors: r.error?.errors }));
