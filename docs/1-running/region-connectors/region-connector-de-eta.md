# Region Connector for ETA (Germany)

This region connector allows the EDDIE platform to interact with the German ETA Plus standard.

## Registration and User Setup

Before requesting an account, please prepare the following information:
- Company name
- Technical contact person (full name)
- Contact email address
- Intended purpose (e.g., EDDIE - German Region Connector)

1. Submit the required information to the ETA+ contact email info@etaplus.energy.
2. The platform administrator creates the user account.
3. Login and other credentials will be securely shared with your technical contact.
4. Perform an initial login to validate access.

As accounts are manually provisioned, the turnaround time is typically 1-2 business days.

## Configuration of the Region Connector

The region connector needs a set of configuration values to function properly. You can configure these values using Spring properties or environment variables.

| Configuration values                           | Description                                                                   |
|------------------------------------------------|-------------------------------------------------------------------------------|
| `region-connector.de.eta.auth.client-id`       | The OAuth client ID.                                                          |
| `region-connector.de.eta.auth.token-url`       | The OAuth token endpoint URL.                                                 |
| `region-connector.de.eta.auth.authorization-url` | The OAuth authorization endpoint URL.                                         |
| `region-connector.de.eta.auth.redirect-uri`    | The OAuth redirect URI for handling callbacks.                                |
| `region-connector.de.eta.auth.scope`           | The OAuth scopes required for authorization.                                  |


When using environment variables, replace all non-alphanumeric characters with an underscore (`_`) and convert letters to uppercase (e.g., `REGION_CONNECTOR_DE_ETA_ELIGIBLE_PARTY_ID`).

```properties :spring
region-connector.de.eta.auth.client-id=YOUR_OAUTH_CLIENT_ID
region-connector.de.eta.auth.token-url=https://auth.eta-plus.de/token
region-connector.de.eta.auth.authorization-url=https://auth.eta-plus.de/authorize
region-connector.de.eta.auth.redirect-uri=${eddie.public.url}/region-connectors/de-eta/callback
region-connector.de.eta.auth.scope=openid
```
