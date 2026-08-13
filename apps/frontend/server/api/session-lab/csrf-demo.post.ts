export default defineEventHandler(async (event) => {
  await requireMirrorLabSession(event)
  const header = getHeader(event, 'x-csrf-token')
  const cookie = csrfTokenFrom(event)
  if (!header || !cookie || header !== cookie) {
    return problemJson(event, 403, 'csrf_failed', 'CSRF token missing or does not match cookie')
  }
  return { status: 'ok' }
})
