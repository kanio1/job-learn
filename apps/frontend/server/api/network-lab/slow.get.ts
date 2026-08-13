export default defineEventHandler(async (event) => {
  requireMirrorLab(event)
  await new Promise(resolve => setTimeout(resolve, 8000))
  return { status: 'ok' }
})
