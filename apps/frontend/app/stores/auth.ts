export const useAuthStore = defineStore('auth', () => {
  const session = useUserSession()

  const isAuthenticated = computed(() => session.loggedIn.value)
  const user = computed(() => session.user.value)

  async function login() {
    await navigateTo('/auth/keycloak', { external: true })
  }

  async function logout() {
    await session.clear()
    await navigateTo('/login')
  }

  return { isAuthenticated, user, login, logout }
})
