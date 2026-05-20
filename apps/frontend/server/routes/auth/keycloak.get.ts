export default defineOAuthOidcEventHandler({
  config: {
    clientId: useRuntimeConfig().oauth.oidc.clientId,
    clientSecret: useRuntimeConfig().oauth.oidc.clientSecret,
    openidConfig: useRuntimeConfig().oauth.oidc.openidConfig,
    scope: ['openid', 'profile', 'email'],
    redirectURL: useRuntimeConfig().oauth.oidc.redirectURL
  },
  async onSuccess(event, { user, tokens }) {
    await setUserSession(event, {
      user: {
        username: user.preferred_username ?? user.name ?? user.sub,
        email: user.email
      },
      secure: {
        accessToken: tokens.access_token
      },
      loggedInAt: Date.now()
    })

    return sendRedirect(event, '/admin/merchants')
  },
  onError(event, error) {
    console.error('Keycloak OIDC login error', error)
    return sendRedirect(event, '/login?error=keycloak')
  }
})
