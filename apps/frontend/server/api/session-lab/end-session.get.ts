export default defineEventHandler(async (event) => {
  await requireMirrorLabSession(event)
  const endSessionUrl = oidcEndSessionUrl(event)
  await clearUserSession(event)
  return sendRedirect(event, endSessionUrl, 302)
})
