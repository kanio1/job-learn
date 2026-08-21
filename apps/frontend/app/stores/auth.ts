export const useAuthStore = defineStore('auth', () => {
  const session = useUserSession()

  const isAuthenticated = computed(() => session.loggedIn.value)
  const user = computed(() => session.user.value)

  async function login() {
    await navigateTo('/auth/keycloak', { external: true })
  }

  async function logout() {
    // Same-origin GET 302 → Keycloak end_session. Do not POST-then-navigate:
    // clearing nuxt-session first lets the SPA land on /login before the hop.
    await navigateTo('/api/auth/end-session', { external: true })
  }

  async function logoutShallow() {
    await session.clear()
    await navigateTo('/login?logout=shallow')
  }

  return { isAuthenticated, user, login, logout, logoutShallow }
})
