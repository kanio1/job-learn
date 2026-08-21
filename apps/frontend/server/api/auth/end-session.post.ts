export default defineEventHandler(async (event) => {
  await requireUserSession(event)
  return endOidcSession(event)
})
