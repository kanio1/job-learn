export default defineNitroPlugin(() => {
  if (process.env.NUXT_SESSION_COOKIE_SECURE !== 'true') {
    return
  }
  const password = process.env.NUXT_SESSION_PASSWORD ?? ''
  if (password.length < 32) {
    throw new Error(
      'NUXT_SESSION_PASSWORD must be at least 32 characters when NUXT_SESSION_COOKIE_SECURE=true',
    )
  }
})
