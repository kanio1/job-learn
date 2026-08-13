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
  merchantManager: `${authDir}/merchant-manager.json`,
} as const
