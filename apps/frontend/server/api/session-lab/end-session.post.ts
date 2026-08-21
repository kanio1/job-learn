export default defineEventHandler(async (event) => {
  await requireMirrorLabSession(event)
  return endOidcSession(event)
})
