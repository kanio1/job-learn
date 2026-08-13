export default defineEventHandler(async (event) => {
  await requireMirrorLabSession(event)
  return {
    cookies: [
      {
        name: 'nuxt-session',
        httpOnly: true,
        secure: false,
        sameSite: 'lax',
        path: '/',
        jsVisible: false,
      },
    ],
    note: 'document.cookie cannot list HttpOnly cookies. Playwright context.cookies() is the oracle. storageState is not sessionStorage.',
  }
})
