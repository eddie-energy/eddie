# Keycloak

Authentication is not handled by AIIDA itself but delegated to Keycloak. 
[Keycloak](https://www.keycloak.org/) is an open-source Identity and Access Management (IAM) solution for modern applications and services. 
It offers features such as Single Sign-On (SSO), user federation, identity brokering, and social login. 
Supporting standard protocols like OAuth2, OpenID Connect, and SAML 2.0, Keycloak ensures compatibility with a wide range of applications.

AIIDA can be configured to use either a central cloud-based Keycloak instance or a locally deployed one. 
When AIIDA runs in the cloud, the role of Keycloak becomes even more significant - allowing the same user accounts and authentication system to seamlessly access multiple AIIDA instances or other EDDIE applications.

## How is it used?

The docker-compose file in the `aiida/docker` directory of the repository includes a keycloak instance with a preconfigured user named
`aiida`, with the password `aiida`.

The preconfigured Keycloak includes an EDDIE realm with the AIIDA client, that is used for authentication.
The client secret of the AIIDA client is set to `REPLACE_ME` and can be regenerated in the admin console, 
which is reachable at http://localhost:8888. The keycloak admin user is configured in the `.env` file and has by default the username
`admin` and the password `admin`.

If a different Keycloak instance should be used, it can be configured in the `application.yml` file or using
environment variables.

When AIIDA is started locally for development, it can lead to unexpected logouts, since both the example app and AIIDA
use the same session ID (JSESSIONID) per default.
To overcome this issue, the property `server.servlet.session.cookie.name` can be set to
`AIIDA_SESSION_ID`, which will fix the unexpected behavior.  
**Important:**
This is only relevant during development, because usually AIIDA and EDDIE services are not deployed using the same host
(localhost for the case of development).

### Keycloak Integration in Docker Network

To enable Keycloak usage within a Docker network, several configurations had to be made in the
`application.yml` file of the Spring application.
When setting the property `issuer-uri` in the `application.yml`, the application retrieves the URIs from
`http://localhost:8888/realms/AIIDA/.well-known/openid-configuration`.
Since this URI is not accessible within the Docker network, the required URIs must be defined explicitly.

The following properties must be set in the `application.yml` file:

| Property            | Description                                                   |
|---------------------|---------------------------------------------------------------|
| authorization-uri   | URI for redirecting users for authorization.                  |
| token-uri           | URI to exchange the access code for an access token.          |
| user-info-uri       | URI to fetch user information.                                |
| jwk-set-uri         | URI to obtain the public key for verifying JWTs.              |
| user-name-attribute | JWT claim that contains the username.                         |
| redirect-uri        | URI for redirecting users back to the application post-login. |
| end-session-uri     | URI for logging out of Keycloak sessions.                     |

Additionally, Keycloak requires a configured frontend URL to validate the issuer URI. This is specified using the
`KC_HOSTNAME` variable in the `compose.yml` file.
The provided
`compose.yml` file provides a preconfiguration of these values for keycloak, you can configure it using the environments:

- `AIIDA_PUBLIC_URL`
- `KEYCLOAK_INTERNAL_HOST`
- `KEYCLOAK_EXTERNAL_HOST`

For a local development setup these values can be configured as follows (defaults of `.env` file):

- `AIIDA_EXTERNAL_HOST=http://localhost:8080`
- `KEYCLOAK_INTERNAL_HOST=http://keycloak:8080`
- `KEYCLOAK_EXTERNAL_HOST=http://localhost:8888`

For a production deployment setup these values can be configured as follows assuming keycloak is running on
`keycloak.eddie.energy` and aiida is running on `aiida.eddie.energy`:

- `AIIDA_EXTERNAL_HOST=https://aiida.eddie.energy`
- `KEYCLOAK_INTERNAL_HOST=https://keycloak.eddie.energy`
- `KEYCLOAK_EXTERNAL_HOST=https://keycloak.eddie.energy`

## Additional Info 

### EDDIE Keycloak Theme

The current version of the AIIDA keycloak theme can be found in the [keycloak-eddie-theme/themes/aiida-theme](https://github.com/eddie-energy/eddie/tree/main/keycloak-eddie-theme/themes/aiida-theme) folder.
The theme customizes the login screen to match the style of AIIDA.