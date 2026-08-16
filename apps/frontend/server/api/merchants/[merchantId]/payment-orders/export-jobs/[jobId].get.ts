export default defineEventHandler(async (event): Promise<any> => {
  const merchantId = getRouterParam(event, 'merchantId')
  const jobId = getRouterParam(event, 'jobId')
  return backendApi(event, `/api/merchants/${merchantId}/payment-orders/export-jobs/${jobId}`)
})
