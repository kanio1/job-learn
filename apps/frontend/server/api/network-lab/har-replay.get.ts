export default defineEventHandler((event) => {
  requireMirrorLab(event)
  return { source: 'live' }
})
