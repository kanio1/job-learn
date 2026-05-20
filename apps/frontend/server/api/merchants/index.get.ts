export default defineEventHandler(async (event) => {
  return backendApi(event, '/api/merchants')
})
