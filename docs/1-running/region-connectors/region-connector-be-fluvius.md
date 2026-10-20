# Region Connector for Belgium (Fluvius)

This README will guide you through the process of configuring a region connector for flemish Belgium.

## Prerequisites

- Register as a partner with [Fluvius](https://partner.fluvius.be/nl/energiedienstverleners), a belgian company ID is required for this.
- During the onboarding process, a certificate has to be provided, which fluvius uses to allowlist your application.
- Furthermore, credentials are provided to access the API.

## Configuration of the Region Connector

The region connector needs a set of configuration values to be able to function correctly, how you provide these values
depends on the way you deploy the region connector.

| Configuration values                                      | Description                                                                                                                                                      |
|-----------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `region-connector.be.fluvius.oauth.token-url`             | The URL to obtain an API token, again provided by fluvius during the onboarding.                                                                                 |
| `region-connector.be.fluvius.oauth.client-id`             | Client ID to obtain an API token, provided by Fluvius.                                                                                                           |
| `region-connector.be.fluvius.oauth.client-secret`         | Client secret for sandbox authentication. Required only with `client-secret` authentication.                                                                     |
| `region-connector.be.fluvius.oauth.authentication-method` | `client-secret` (default) for sandbox, or `certificate` for live OAuth authentication.                                                                           |
| `region-connector.be.fluvius.oauth.ssl-bundle`            | Spring SSL bundle containing the registered certificate and private key. Default: `fluvius`.                                                                     |
| `region-connector.be.fluvius.oauth.tenant-id`             | The tenant ID identifies the microsoftonline OAuth server.                                                                                                       |
| `region-connector.be.fluvius.oauth.scope`                 | Scope of the requested API token, provided by Fluvius.                                                                                                           |
| `region-connector.be.fluvius.base-url`                    | The base-url of the API, the urls are provided during the onboarding process by Fluvius.                                                                         |
| `region-connector.be.fluvius.subscription-key`            | The subscription key to access the API, provided by Fluvius.                                                                                                     |
| `region-connector.be.fluvius.contract-number`             | Contract number to create permission requests via Fluvius' API, provided by Fluvius.                                                                             |
| `region-connector.be.fluvius.mock-mandates`               | Is either `true` or `false`. Set to `true` if the sandbox environment provided by Fluvius is used. Can be used to test the region connector against the sandbox. |
| `region-connector.be.fluvius.redirect-uri`                | The redirect uri that's used for success and error redirects by fluvius.                                                                                         |
| `region-connector.be.fluvius.retry`                       | Configures when a failed to send permission request should be retried. Uses Spring Cron syntax. Default is `0 0 * * * *`.                                        |
| `region-connector.be.fluvius.check-acceptance`            | Configures when the region connector should check for acceptance of permission requests. Uses Spring Cron syntax. Default is `0 0 * * * *`.                      |
| `spring.ssl.bundle.pem.fluvius.keystore.certificate`      | Certificate registered with Fluvius for live OAuth authentication. Optional for sandbox.                                                                         |
| `spring.ssl.bundle.pem.fluvius.keystore.private-key`      | Private key matching the registered certificate. Required for live OAuth authentication.                                                                         |

The region connector can be configured using Spring properties or environment variables.
When using environment variables, the configuration values need to be converted in the following way:

- Replace all non-alphanumeric characters with an underscore (`_`)
- Optionally convert all letters to upper case

```properties :spring
region-connector.be.fluvius.base-url=https://apihub.fluvius.be/esco-sbx
region-connector.be.fluvius.oauth.authentication-method=client-secret
region-connector.be.fluvius.oauth.token-url=https://login.microsoftonline.com/${region-connector.be.fluvius.oauth.tenant-id}/oauth2/V2.0/token
region-connector.be.fluvius.oauth.client-id=REPLACE_ME
region-connector.be.fluvius.oauth.tenant-id=REPLACE_ME
region-connector.be.fluvius.oauth.client-secret=REPLACE_ME
region-connector.be.fluvius.oauth.scope=REPLACE_ME
region-connector.be.fluvius.subscription-key=REPLACE_ME
region-connector.be.fluvius.contract-number=REPLACE_ME
region-connector.be.fluvius.mock-mandates=false
region-connector.be.fluvius.redirect-uri=${eddie.public.url}
region-connector.be.fluvius.retry=0 0 * * * *
region-connector.be.fluvius.check-acceptance=0 0 * * * *
```

## Live API authentication

The sandbox and live APIs use different client IDs and authentication methods.
The sandbox uses a client secret. The live API uses the registered certificate and its private key to sign an OAuth client assertion.
A certificate thumbprint alone is insufficient; the private key must be available to EDDIE.

For live access, use the common settings above and replace the sandbox-specific settings with:

```properties :spring
region-connector.be.fluvius.base-url=https://apihub.fluvius.be/esco-live
region-connector.be.fluvius.oauth.authentication-method=certificate
region-connector.be.fluvius.oauth.client-id=REPLACE_WITH_LIVE_CLIENT_ID
region-connector.be.fluvius.oauth.ssl-bundle=fluvius
region-connector.be.fluvius.mock-mandates=false
spring.ssl.bundle.pem.fluvius.keystore.certificate=file:/path/to/certificate.pem
spring.ssl.bundle.pem.fluvius.keystore.private-key=file:/path/to/private-key.pem
```

No `oauth.client-secret` is required in certificate mode.
Spring SSL bundles also support JKS/PKCS12 keystores; configure `spring.ssl.bundle.jks.fluvius` instead of the PEM properties when using those formats.
For a keystore containing multiple private keys, specify `spring.ssl.bundle.jks.fluvius.key.alias`.
Certificate credentials are validated at startup. Tokens are cached using the token endpoint's `expires_in` value and renewed before expiry.
The configured SSL bundle is also applied to the API HTTP client when present. Sandbox authentication works without a certificate bundle.

Use an HTTPS redirect URI whose root is allowlisted by Fluvius.
API requests must originate from the EEA or UK.
Mock mandates are available only on the sandbox API; configuring them against the live endpoint is rejected.
Sandbox short URLs cannot be used on Mijn Fluvius. The customer consent flow requires the live API and an actual customer approval.

Short-URL requests use `sso=false` because EDDIE does not establish an active FAS/CSAM session for the customer.

## Running the Region Connector via EDDIE

If you are using EDDIE, the region connector should appear in the list of available
region connectors if it has been configured correctly.
