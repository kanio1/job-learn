export function useIdleLock() {
  const config = useRuntimeConfig()
  const enabled = computed(() => config.public.mirrorLabEnabled === true)
  const idleSeconds = computed(() => {
    const raw = Number(config.public.mirrorLabIdleSeconds)
    return Number.isFinite(raw) && raw > 0 ? raw : 120
  })
  const locked = ref(false)
  const lastActivity = ref(Date.now())
  let timer: ReturnType<typeof setInterval> | undefined

  function touch() {
    if (locked.value) {
      return
    }
    lastActivity.value = Date.now()
  }

  function tick() {
    if (!enabled.value || locked.value) {
      return
    }
    if (Date.now() - lastActivity.value >= idleSeconds.value * 1000) {
      locked.value = true
    }
  }

  onMounted(() => {
    if (!import.meta.client || !enabled.value) {
      return
    }
    lastActivity.value = Date.now()
    window.addEventListener('pointerdown', touch, { passive: true })
    window.addEventListener('keydown', touch)
    timer = setInterval(tick, 1000)
  })

  onBeforeUnmount(() => {
    if (!import.meta.client) {
      return
    }
    window.removeEventListener('pointerdown', touch)
    window.removeEventListener('keydown', touch)
    if (timer) {
      clearInterval(timer)
    }
  })

  return { locked, idleSeconds, enabled }
}
