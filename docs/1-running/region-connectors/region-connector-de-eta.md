# Region Connector for ETA (Germany)

This region connector allows the EDDIE platform to interact with the German ETA Plus standard.
It uses a custom OAuth-like authentication flow for permission requests and a separate client credentials
pair to authenticate API calls for metered data retrieval.

## Prerequisites

Before requesting an account, please prepare the following information:
- Company name
- Technical contact person (full name)
- Contact email address
- Intended purpose (e.g., EDDIE - German Region Connector)

1. Submit the required information to the ETA+ contact email info@etaplus.energy.
2. The platform administrator creates the user account.
3. Login and other credentials will be securely shared with your technical contact.
4. Perform an initial login to validate access.
5. Obtain the client id/secret for the authentication flow and the API client id/secret for the
   metered-data API from the ETA+ portal.

As accounts are manually provisioned, the turnaround time is typically 1-2 business days.

## Configuration of the Region Connector

The region connector needs a set of configuration values to be able to function correctly, how you provide these values
depends on the way you deploy the region connector.

| Configuration values                                         | Description                                                                                                                                                                  |
|--------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `region-connector.de.eta.eligible-party-id`                  | The eligible party identifier assigned to your organization by ETA+. Used to tag outgoing permission requests and data messages.                                             |
| `region-connector.de.eta.api-base-url`                       | The base URL of the ETA+ API. Defaults to `https://int.eta-plus.com/api` (integration environment). Use the production URL once your account is approved for production.    |
| `region-connector.de.eta.api-client-id`                      | The client ID used to authenticate outgoing API requests for metered data. Provided by ETA+.                                                                                 |
| `region-connector.de.eta.api-client-secret`                  | The client secret used to authenticate outgoing API requests for metered data. Provided by ETA+.                                                                             |
| `region-connector.de.eta.metered-data-endpoint`              | Path (appended to `api-base-url`) of the metered-data endpoint. Defaults to `/meters/historical`.                                                                            |
| `region-connector.de.eta.permission-check-endpoint`          | Path (appended to `api-base-url`) of the permission check endpoint. Defaults to `/v1/permissions/{id}`.                                                                      |
| `region-connector.de.eta.response-timeout-seconds`           | Timeout in seconds for individual API requests. Defaults to `30`.                                                                                                            |
| `region-connector.de.eta.retry-max-attempts`                 | Maximum number of retry attempts when the ETA+ API returns a rate limit or transient error. Defaults to `3`.                                                                 |
| `region-connector.de.eta.retry-initial-backoff-seconds`      | Initial backoff delay (in seconds) between retries. The delay increases exponentially with each attempt. Defaults to `2`.                                                    |
| `region-connector.de.eta.ssl-enabled`                        | Whether SSL must be enforced for outgoing requests. Must be consistent with the protocol in `api-base-url` (HTTPS → true, HTTP → false). Defaults to `true`.                 |
| `region-connector.de.eta.ssl-trust-all`                      | If `true`, the client accepts any server certificate. Intended for testing against self-signed integration environments only. Defaults to `false`.                           |
| `region-connector.de.eta.auth.client-id`                     | The client ID used to initiate the authentication flow with ETA+. See [Authentication Flow Details](#authentication-flow-details) below.                                     |
| `region-connector.de.eta.auth.client-secret`                 | The client secret used to exchange the authorization code for an access token.                                                                                                |
| `region-connector.de.eta.auth.token-url`                     | The token endpoint URL of ETA+. For the integration environment: `https://int.eta-plus.com/api/account/oauth/access-grant`.                                                  |
| `region-connector.de.eta.auth.authorization-url`             | The authorization endpoint URL of ETA+. For the integration environment: `https://int.eta-plus.com/#/oauth-redirect`.                                                        |
| `region-connector.de.eta.auth.redirect-uri`                  | The redirect URI registered with ETA+ that users are sent to after authorization. Must point to `{EDDIE_PUBLIC_URL}/region-connectors/de-eta/authorization-callback`.        |
| `region-connector.de.eta.auth.scope`                         | The scope required for the authentication flow. Should be set to `metered-data`.                                                                                             |

The region connector can be configured using Spring properties or environment variables.
When using environment variables, the configuration values need to be converted in the following way:

- Replace all non-alphanumeric characters with an underscore (`_`)
- Optionally convert all letters to upper case

```properties :spring
region-connector.de.eta.eligible-party-id=YOUR_ELIGIBLE_PARTY_ID
region-connector.de.eta.api-base-url=https://int.eta-plus.com/api
region-connector.de.eta.api-client-id=YOUR_API_CLIENT_ID
region-connector.de.eta.api-client-secret=YOUR_API_CLIENT_SECRET
region-connector.de.eta.retry-max-attempts=3
region-connector.de.eta.retry-initial-backoff-seconds=2
region-connector.de.eta.ssl-enabled=true
region-connector.de.eta.ssl-trust-all=false

# Authentication flow (custom OAuth-like, see docs below)
region-connector.de.eta.auth.client-id=YOUR_OAUTH_CLIENT_ID
region-connector.de.eta.auth.client-secret=YOUR_OAUTH_CLIENT_SECRET
region-connector.de.eta.auth.token-url=https://int.eta-plus.com/api/account/oauth/access-grant
region-connector.de.eta.auth.authorization-url=https://int.eta-plus.com/#/oauth-redirect
region-connector.de.eta.auth.redirect-uri=${eddie.public.url}/region-connectors/de-eta/authorization-callback
region-connector.de.eta.auth.scope=metered-data
```

## Running the Region Connector via EDDIE

If you are using EDDIE, the region connector should appear in the list of available
region connectors if it has been configured correctly.

## Authentication Flow Details

> **Important:** The ETA+ authentication endpoints use OAuth 2.0 vocabulary and response formats
> but do **not** implement any of the standard OAuth 2.0 flows (Authorization Code, ROPC, Client
> Credentials) as defined in [RFC 6749](https://datatracker.ietf.org/doc/html/rfc6749). The
> following section documents the actual behavior.

### Actual ETA+ Authentication Flow

```
POST /oauth              (username + password  →  GUID "code" stored in-memory)
     ↓
POST /oauth/access-grant (code  →  access_token + refresh_token as JWT)
     ↓
GET  /oauth/refresh/{token}  (refresh_token  →  new access_token)
```

For comparison, a standard OAuth 2.0 Authorization Code flow looks like this:

```
GET  /authorize          (→ consent page → redirect with ?code=...)
     ↓
POST /token              (code + client_id + client_secret  →  access_token + refresh_token)
     ↓
POST /token              (grant_type=refresh_token  →  new access_token)
```

### Key Differences from Standard OAuth 2.0

| Aspect | Standard OAuth 2.0 | ETA+ Implementation |
|---|---|---|
| **Authorization** | User grants consent on a dedicated authorization page; browser redirects back with a code | Authorization code is obtained directly via `POST /oauth` with username + password (no consent page, no redirect) |
| **Client authentication** | Token exchange requires `client_id` + `client_secret` | `/oauth/access-grant` does not validate `client_id`; `/oauth/token` checks `client_id` but there is no `client_secret` |
| **Token exchange** | Single `POST /token` endpoint with `grant_type` parameter | Two near-identical endpoints: `/oauth/access-grant` (older, less strict) and `/oauth/token` (newer, validates `client_id` and `redirect_uri`) |
| **Refresh** | `POST /token` with `grant_type=refresh_token` in the request body | `GET /oauth/refresh/{token}` with the refresh token as a URL path parameter |
| **JWT claims** | Standard claims (`sub`, `aud`, `iat`, `jti`, etc.) | Custom JWT implementation using HS512 with non-standard claim set |
| **Code storage** | Persistent store (database) | In-memory cache — codes are lost on application restart or in multi-instance deployments |

### Implications for Integration

- **Do not assume standard OAuth 2.0 client library compatibility.** The region connector uses a
  custom authentication client tailored to the ETA+ endpoints.
- **Token refresh uses GET, not POST.** The refresh token is sent as a URL path segment, which
  means it may appear in server access logs and browser history. This is a known deviation.
- **No `client_secret` validation on the primary endpoint.** The `/oauth/access-grant` endpoint
  does not verify `client_id`, so the authorization code alone is sufficient to obtain tokens.
