import { expect, type Request } from '@playwright/test'

type HasHeaders = {
  headers(): Record<string, string>
}

export function requestHeader(request: Request, name: string): string | undefined {
  return request.headers()[name.toLowerCase()]
}

export function expectNoAuthorizationInNetworkResponse(response: HasHeaders): void {
  expect(
    response.headers()['authorization'],
    'Authorization must never appear in a browser-visible response',
  ).toBeUndefined()
}

export function expectNoTokenInText(content: string, label: string): void {
  expect(content.includes('Bearer '), `${label} must not contain Bearer`).toBe(false)
  expect(content.includes('eyJ'), `${label} must not contain a JWT prefix`).toBe(false)
  expect(content.toLowerCase().includes('authorization:'), `${label} must not contain Authorization`).toBe(false)
}
