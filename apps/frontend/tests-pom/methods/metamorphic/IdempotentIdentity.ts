/**
 * Metamorphic — create identity (MR-IDEM, MR-UNIQ).
 *
 * MR-IDEM: same key + same body ⇒ same paymentOrderId (200).
 * MR-UNIQ: same amount/currency, new key+ref ⇒ two 201 and different IDs.
 * Layer: REST. Seed: factory on ALPHA_001. Never seed-learning.
 */

export const mrIdem = { id: 'MR-IDEM', replayStatus: 200 as const }
export const mrUniq = { id: 'MR-UNIQ', secondStatus: 201 as const }
