import Keycloak from 'keycloak-js'

export const keycloak = new Keycloak({
  url: 'https://eddie-demo.projekte.fh-hagenberg.at/iam/',
  realm: 'AIIDA',
  clientId: 'aiida-client',
})
