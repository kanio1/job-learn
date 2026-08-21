import './ipv4-first'

export function requiredEnv(name: string): string {
  const value = process.env[name]
  if (!value) {
    throw new Error(`${name} is required. Live POM tests take credentials only from the environment.`)
  }
  return value
}

export function optionalEnv(name: string, fallback: string): string {
  const value = process.env[name]
  return value && value.length > 0 ? value : fallback
}

const authDir = optionalEnv('PLAYWRIGHT_POM_AUTH_DIR', 'tests-pom/.auth')

export const pomAuthFiles = {
  platformAdmin: `${authDir}/platform-admin.json`,
  platformOperator: `${authDir}/platform-operator.json`,
  platformAdminSession: `${authDir}/platform-admin-session.json`,
  tenantAdmin: `${authDir}/tenant-admin.json`,
  merchantManager: `${authDir}/merchant-manager.json`,
  supportAgent: `${authDir}/support-agent.json`,
  readOnlyUser: `${authDir}/read-only-user.json`,
  merchantDenied: `${authDir}/merchant-denied.json`,
} as const

export function workerManagerAuthFile(index: number): string {
  return `${authDir}/w${index}-merchant-manager.json`
}

/**
 * Browser / OIDC origin. Must match `NUXT_OAUTH_OIDC_REDIRECT_URL`
 * (default `http://localhost:3000/auth/keycloak`). 127.0.0.1 is a different
 * cookie host and produces `Oidc login failed: state mismatch`.
 */
export function pomBrowserBaseURL(): string {
  return optionalEnv('PLAYWRIGHT_BASE_URL', 'http://localhost:3000')
}

/**
 * Node REST origin must match the browser cookie host (localhost after OIDC).
 * `ipv4-first` makes Node resolve localhost to 127.0.0.1 so this is not ::1.
 */
export function pomNodeBaseURL(): string {
  return pomBrowserBaseURL()
}

/** @deprecated Use pomBrowserBaseURL (UI) or pomNodeBaseURL (BffClient). */
export function pomBaseURL(): string {
  return pomBrowserBaseURL()
}
