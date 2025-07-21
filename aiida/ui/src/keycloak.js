import Keycloak from 'keycloak-js'

const { VITE_KEYCLOAK_URL, VITE_KEYCLOAK_REALM, VITE_KEYCLOAK_CLIENT } = import.meta.env

export const keycloak = new Keycloak({
  url: THYMELEAF_KEYCLOAK_URL ?? VITE_KEYCLOAK_URL,
  realm: THYMELEAF_KEYCLOAK_REALM ?? VITE_KEYCLOAK_REALM,
  clientId: THYMELEAF_KEYCLOAK_CLIENT ?? VITE_KEYCLOAK_CLIENT,
})
