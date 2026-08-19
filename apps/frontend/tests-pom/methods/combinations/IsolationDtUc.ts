/**
 * Combination DT + UC (GAP-W2-10 / lekcja 8).
 *
 * Why together: DT says who/what code; UC says which UI path.
 * DT alone = Postman. UC alone = manager 403 misread as tenant isolation.
 * What changes: storageState. Same MerchantsListPage / BffClient.
 * Layer: e2e+rest. Seed: contract ~104. Never seed-learning.
 */

import { merchantAccessMatrix } from '../decision-table/MerchantAccessMatrix'

export const isolationDtUcE2eRows = merchantAccessMatrix.filter(row =>
  row.id === 'SCN-ISO-06' || row.id === 'SCN-ISO-01' || row.id === 'SCN-ISO-09',
)

export const isolationDtUcRestRows = merchantAccessMatrix.filter(row =>
  row.id === 'SCN-ISO-03' || row.id === 'SCN-ISO-02' || row.id === 'SCN-ISO-10',
)
