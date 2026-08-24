import { backendApi } from '~~/server/utils/backendApi'
import { readBody, defineEventHandler } from 'h3'

export default defineEventHandler(async (event) => {
  const body = await readBody(event)
  return backendApi(event, '/api/event-lab/inject/duplicate', { method: 'POST', body })
})
