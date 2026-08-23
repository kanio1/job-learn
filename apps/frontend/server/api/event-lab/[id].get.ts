import { backendApi } from '~~/server/utils/backendApi'

export default defineEventHandler(async (event) => {
  const id = event.context.params?.id
  const res = await backendApi(event, `/api/event-lab/${id}`, { method: 'GET' })
  if (res.status === 404) {
    throw createError({ statusCode: 404, statusMessage: 'Not found' })
  }
  return res.data
})
