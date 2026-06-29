/**
 * Wrapper around useToast() that applies semantic CSS classes to each notification
 * so Playwright tests can query by type:
 *   page.locator('.toast-success')   — any success notification
 *   page.locator('.toast-error')     — any error notification
 *   page.locator('[data-slot="title"]').filter({ hasText: /.../ }) — by content
 *
 * The `class` prop is forwarded to the Reka Toast root <li> element by Nuxt UI.
 */
export function useAppToast() {
  const toast = useToast()

  return {
    success: (title: string, description?: string) =>
      toast.add({ title, description, color: 'success', class: 'toast-success' }),

    error: (title: string, description?: string) =>
      toast.add({ title, description, color: 'error', class: 'toast-error' }),

    warning: (title: string, description?: string) =>
      toast.add({ title, description, color: 'warning', class: 'toast-warning' }),

    info: (title: string, description?: string) =>
      toast.add({ title, description, color: 'neutral', class: 'toast-info' }),
  }
}
