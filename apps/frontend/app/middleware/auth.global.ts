/**
 * Global route guard.
 *
 * - Unauthenticated visitors are redirected to /login with the intended route
 *   preserved in ?redirectTo= so they land back after login.
 * - Authenticated visitors are allowed through; per-route 403 handling is done
 *   by the useAuthError composable after each API call.
 * - /login itself redirects authenticated users to /admin/merchants.
 * - /forbidden is accessible to authenticated users (it is the 403 surface).
 * - /psp-redirect-simulator is a standalone mock of an external PSP checkout
 *   page (F-D2) — a real PSP redirect target lives on a different domain
 *   entirely, so it is intentionally outside this app's session realm.
 * - /auth/keycloak is the nuxt-auth-utils OIDC start + callback. Do not skip
 *   the whole /auth/ prefix — a future /auth/* page would otherwise bypass the
 *   session guard.
 *
 * Feature: iam-roles-and-keycloak-login
 */
function isOidcHandlerPath(path: string): boolean {
  return path === '/auth/keycloak' || path.startsWith('/auth/keycloak/')
}

export default defineNuxtRouteMiddleware(async (to) => {
  const session = useUserSession()

  // Standalone PSP simulator — no session realm, same as a real external PSP page.
  if (to.path === '/psp-redirect-simulator' || to.path.startsWith('/psp/checkout/') || to.path === '/checkout-lab/return' || isOidcHandlerPath(to.path)) {
    return
  }

  const mirrorLabEnabled = useRuntimeConfig().public.mirrorLabEnabled === true
  const mirrorLabPrefixes = [
    '/admin/mirror-lab',
    '/admin/session-lab',
    '/admin/visual-lab',
    '/admin/network-lab',
    '/consent/mirror-lab',
  ]
  if (!mirrorLabEnabled && mirrorLabPrefixes.some(prefix => to.path === prefix || to.path.startsWith(`${prefix}/`))) {
    throw createError({ statusCode: 404, statusMessage: 'Not Found' })
  }
  const rlsLabEnabled = useRuntimeConfig().public.rlsLabEnabled === true
  if (!rlsLabEnabled && (to.path === '/admin/rls-lab' || to.path.startsWith('/admin/rls-lab/'))) {
    throw createError({ statusCode: 404, statusMessage: 'Not Found' })
  }

  if (!session.ready.value) {
    await session.fetch()
  }

  // Let the login page through for unauthenticated users.
  // Authenticated users on /login are redirected to the dashboard.
  if (to.path === '/login') {
    if (session.loggedIn.value) {
      return navigateTo('/admin/merchants')
    }
    return
  }

  // /forbidden is the 403 surface — only authenticated users should see it.
  // Unauthenticated users hitting /forbidden get the standard login redirect.
  if (!session.loggedIn.value) {
    // Capture the intended destination so the user lands back after login.
    const redirectTo = to.fullPath !== '/login' ? to.fullPath : undefined
    const target = redirectTo
      ? `/login?redirectTo=${encodeURIComponent(redirectTo)}`
      : '/login'
    return navigateTo(target)
  }
})
