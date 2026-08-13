export default defineEventHandler(async (event) => {
  requireMirrorLab(event)
  const body = await readBody(event)
  const stepUp = getHeader(event, 'x-lab-step-up')
  const headers: Record<string, string> = {}
  if (stepUp) {
    headers['X-Lab-Step-Up'] = stepUp
  }
  return backendApi(event, '/api/mirror-lab/high-value-refunds', { method: 'POST', body, headers })
})
