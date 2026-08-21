export default defineEventHandler(async (event) => {
  const parts = await readMultipartFormData(event)
  const file = parts?.find(part => part.name === 'file')
  if (!file?.data) {
    setResponseStatus(event, 400)
    setHeader(event, 'Content-Type', 'application/problem+json')
    return {
      type: 'https://api.payment-quality.local/problems/validation',
      title: 'Bad Request',
      status: 400,
      detail: 'CSV file is required',
      error: 'validation',
    }
  }

  const form = new FormData()
  const blob = new Blob([file.data], { type: file.type || 'text/csv' })
  form.append('file', blob, file.filename || 'merchants.csv')
  return backendApi(event, '/api/merchants/import/preview', {
    method: 'POST',
    body: form,
  })
})
