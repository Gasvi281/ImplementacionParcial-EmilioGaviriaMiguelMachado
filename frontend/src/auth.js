import Keycloak from 'keycloak-js'

export const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'camel-dwarf-racing',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'racing-frontend',
})

export function initKeycloak() {
  return keycloak.init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false })
}

export function getRoles() {
  return keycloak.tokenParsed?.realm_access?.roles ?? []
}

export function hasRole(...roles) {
  const mine = getRoles()
  return roles.some((role) => mine.includes(role))
}

export function getUsername() {
  return keycloak.tokenParsed?.preferred_username ?? 'usuario'
}

export function logout() {
  keycloak.logout({ redirectUri: window.location.origin })
}
