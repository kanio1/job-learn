export default defineEventHandler(async (event) => {
  await requireUserSession(event)
  const endSessionUrl = oidcEndSessionUrl(event)
  await clearUserSession(event)
  return sendRedirect(event, endSessionUrl, 302)
})
