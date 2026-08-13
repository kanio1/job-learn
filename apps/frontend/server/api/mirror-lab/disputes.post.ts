export default defineEventHandler(async (event) => {
  requireMirrorLab(event)
  const body = await readBody(event)
  return backendApi(event, '/api/mirror-lab/disputes', { method: 'POST', body })
})
