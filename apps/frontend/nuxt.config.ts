export default defineNuxtConfig({
  modules: ['@nuxt/ui', '@pinia/nuxt', 'nuxt-auth-utils'],
  css: ['~/assets/css/main.css'],
  devtools: { enabled: true },
  colorMode: {
    preference: 'light',
    fallback: 'light'
  },
  compatibilityDate: '2026-05-18',
  routeRules: {
    '/admin/**': { ssr: false }
  },
  typescript: {
    typeCheck: true
  },
  runtimeConfig: {
    oauth: {
      oidc: {
        clientId: process.env.NUXT_OAUTH_OIDC_CLIENT_ID || 'payment-quality-dashboard',
        clientSecret: process.env.NUXT_OAUTH_OIDC_CLIENT_SECRET || '',
        openidConfig: process.env.NUXT_OAUTH_OIDC_OPENID_CONFIG || 'http://localhost:8081/realms/payment-quality/.well-known/openid-configuration',
        redirectURL: process.env.NUXT_OAUTH_OIDC_REDIRECT_URL || 'http://localhost:3000/auth/keycloak'
      }
    },
    public: {
      apiBaseUrl: process.env.NUXT_PUBLIC_API_BASE_URL || 'http://localhost:8080',
      keycloakUrl: process.env.NUXT_PUBLIC_KEYCLOAK_URL || 'http://localhost:8081',
      keycloakRealm: process.env.NUXT_PUBLIC_KEYCLOAK_REALM || 'payment-quality',
      keycloakClientId: process.env.NUXT_PUBLIC_KEYCLOAK_CLIENT_ID || 'payment-quality-dashboard'
    }
  }
})
