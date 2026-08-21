/**
 * UC-M360-40/41 — CSV preview then commit.
 *
 * What changes: file contents. DB merchants only after commit.
 * Layer: E2E upload + REST commit 409. Seed: unique CSV refs, never Alpha seed.
 */

export const merchantImportJourney = {
  previewThenListUnchanged: 'PW-M360-E2E-080',
  invalidHeader: 'PW-M360-E2E-081',
  emptyFile: 'PW-M360-E2E-082',
  duplicateUk: 'PW-M360-E2E-083',
  commitAppearsInTable: 'PW-M360-E2E-084',
  secondCommitConflict: 'PW-M360-E2E-085',
} as const
