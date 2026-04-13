# Region Connector for ETA (Germany)

This region connector allows the EDDIE platform to interact with the German ETA Plus standard.
It uses an OAuth 2.0 Authorization Code Grant flow for permission requests and a separate client credentials
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
5. Obtain the OAuth client id/secret for the authorization flow and the API client id/secret for the
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
| `region-connector.de.eta.auth.client-id`                     | The OAuth client ID used to initiate the Authorization Code flow with ETA+.                                                                                                  |
| `region-connector.de.eta.auth.client-secret`                 | The OAuth client secret used to exchange the authorization code for an access token.                                                                                         |
| `region-connector.de.eta.auth.token-url`                     | The OAuth token endpoint URL of ETA+. For the integration environment: `https://int.eta-plus.com/api/account/oauth/access-grant`.                                            |
| `region-connector.de.eta.auth.authorization-url`             | The OAuth authorization endpoint URL of ETA+. For the integration environment: `https://int.eta-plus.com/#/oauth-redirect`.                                                  |
| `region-connector.de.eta.auth.redirect-uri`                  | The redirect URI registered with ETA+ that users are sent to after authorization. Must point to `{EDDIE_PUBLIC_URL}/region-connectors/de-eta/authorization-callback`.        |
| `region-connector.de.eta.auth.scope`                         | The OAuth scope required for the authorization flow. Should be set to `metered-data`.                                                                                        |

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

# OAuth authorization flow
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
