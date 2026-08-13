export default defineEventHandler(async (event) => {
  await requireMirrorLabSession(event)
  const token = crypto.randomUUID()
  setCookie(event, 'mrl-csrf', token, {
    httpOnly: false,
    sameSite: 'lax',
    path: '/',
  })
  return { token }
})
