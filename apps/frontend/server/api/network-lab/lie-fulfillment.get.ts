export default defineEventHandler((event) => {
  requireMirrorLab(event)
  return {
    status: 'success',
    warning: 'This JSON is not an oracle. Check persistence or waitForResponse.',
  }
})
