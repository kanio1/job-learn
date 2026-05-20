declare module '#auth-utils' {
  interface User {
    username?: string
    email?: string
  }

  interface UserSession {
    loggedInAt?: number
  }

  interface SecureSessionData {
    accessToken?: string
  }
}

export {}
