type BackendStatus = {
  application: string
  phase: string
  status: string
}

export default defineEventHandler(async (): Promise<BackendStatus> => {
  const config = useRuntimeConfig()
  const backendUrl = config.public.apiBaseUrl || 'http://localhost:8080'
  return await $fetch<BackendStatus>(`${backendUrl}/api/status`)
})
