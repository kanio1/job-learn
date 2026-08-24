import { backendApi } from '~~/server/utils/backendApi'

export default defineEventHandler(async (event) => {
  const id = event.context.params?.id
  return backendApi(event, `/api/event-lab/${id}`, { method: 'GET' })
})
