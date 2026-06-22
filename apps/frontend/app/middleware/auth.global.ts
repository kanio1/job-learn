/**
 * Global route guard.
 *
 * - Unauthenticated visitors are redirected to /login with the intended route
 *   preserved in ?redirectTo= so they land back after login.
 * - Authenticated visitors are allowed through; per-route 403 handling is done
 *   by the useAuthError composable after each API call.
 * - /login itself redirects authenticated users to /admin/merchants.
 * - /forbidden is accessible to authenticated users (it is the 403 surface).
 *
 * Feature: iam-roles-and-keycloak-login
 */
export default defineNuxtRouteMiddleware(async (to) => {
  const session = useUserSession()

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
