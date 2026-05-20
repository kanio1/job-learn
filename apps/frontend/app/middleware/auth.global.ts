export default defineNuxtRouteMiddleware(async (to) => {
  const session = useUserSession()

  if (!session.ready.value) {
    await session.fetch()
  }

  if (to.path === '/login') {
    if (session.loggedIn.value) {
      return navigateTo('/admin/merchants')
    }
    return
  }

  if (!session.loggedIn.value) {
    return navigateTo('/login')
  }
})
