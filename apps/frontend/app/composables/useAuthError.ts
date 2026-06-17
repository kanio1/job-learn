/**
 * useAuthError — deterministic 401-vs-403 reaction layer.
 *
 * Inspects an ApiResponse status after each proxied call and routes to the
 * appropriate surface:
 *   - 401 or session missing → Auth_Required_Redirect to /login
 *   - 403                   → Forbidden surface (/forbidden), never a login redirect
 *   - Other statuses        → unchanged (ProblemDetailsCard / ErrorState)
 *
 * The two reactions are mutually exclusive for every status value.
 *
 * Feature: iam-roles-and-keycloak-login (Property 5)
 */
export function useAuthError() {
  const router = useRouter()
  const route = useRoute()

  /**
   * Call this after every proxied Backend_API call.
   * Returns true when a reaction was triggered (caller should stop processing).
   */
  function handleStatus(status: number): boolean {
    if (status === 401) {
      triggerAuthRequired()
      return true
    }
    if (status === 403) {
      triggerForbidden()
      return true
    }
    return false
  }

  /**
   * Trigger the Auth_Required_Redirect.
   * Captures the intended route so it can be restored after login.
   */
  function triggerAuthRequired(intended?: string): void {
    const target = intended ?? route.fullPath
    router.push(`/login?redirectTo=${encodeURIComponent(target)}`)
  }

  /**
   * Route to the Forbidden page.
   * Never redirects to login — the user IS authenticated, just not authorized.
   */
  function triggerForbidden(): void {
    router.push('/forbidden')
  }

  return { handleStatus, triggerAuthRequired, triggerForbidden }
}
