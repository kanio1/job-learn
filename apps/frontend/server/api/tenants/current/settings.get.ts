export default defineEventHandler(async (event): Promise<any> => {
  return backendApi(event, '/api/tenants/current/settings')
})
