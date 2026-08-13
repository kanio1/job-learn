export default defineEventHandler((event) => {
  requireMirrorLab(event)
  setResponseHeaders(event, {
    'Access-Control-Allow-Origin': 'http://localhost:3000',
    'Access-Control-Allow-Credentials': 'true',
    'Access-Control-Allow-Methods': 'GET,OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type',
  })
  if (event.method === 'OPTIONS') {
    setResponseStatus(event, 204)
    return ''
  }
  return {
    cookieWouldBeSent: 'only if fetch credentials include and origin matches http://localhost:3000',
    hostedNote: 'hosted checkout must not use credentialed cross-origin cookies',
  }
})
