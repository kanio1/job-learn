import { randomUUID } from 'node:crypto'
import type { TestInfo } from '@playwright/test'

export function uniqueToken(): string {
  return randomUUID().replace(/-/g, '').slice(0, 12).toUpperCase()
}

export function uniqueMerchantReference(testInfo: TestInfo): string {
  return `POM-${testInfo.workerIndex}-${uniqueToken()}`.slice(0, 32)
}

export function uniqueOrderReference(testInfo: TestInfo, label = 'PAY'): string {
  return `${label}-${testInfo.workerIndex}-${uniqueToken()}`.slice(0, 64)
}

export function uniqueUsername(testInfo: TestInfo): string {
  return `pom.u.${testInfo.workerIndex}.${uniqueToken().toLowerCase()}`.slice(0, 64)
}

export type LabUserDraft = {
  username: string
  email: string
  temporaryPassword: string
}

export function uniqueLabUser(testInfo: TestInfo): LabUserDraft {
  const username = uniqueUsername(testInfo)
  return {
    username,
    email: `${username}@lab.local`,
    temporaryPassword: `Pom-${username.slice(-8)}1a`,
  }
}

export function uniqueIdempotencyKey(testInfo: TestInfo, label = 'IDEM'): string {
  return `${label}-${testInfo.workerIndex}-${uniqueToken()}`
}

export function uniqueExtOrderId(testInfo: TestInfo): string {
  return `BOOK-${testInfo.workerIndex}-${uniqueToken()}`
}

export function uniqueCaseReference(testInfo: TestInfo): string {
  return `INC-${testInfo.workerIndex}${uniqueToken()}`.slice(0, 32)
}
