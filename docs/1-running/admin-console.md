# Admin Console

The administrative console allows the eligible party to manage permission via a web interface.
It will be served via `<hostname>:<eddie.management.server.port>/outbound-connectors/admin-console`.
The default for `eddie.management.server.port` is `9090`.

The admin console can be configured to require authentication.
Currently, only basic auth and a single admin user is supported.
The username for the admin user and a ***BCrypt encrypted*** password can be configured using properties or environment variables, as described below.

## Configuration of the Admin Console

| Configuration values                                            | Description                                                                                                                               |
|-----------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| `outbound-connector.admin.console.enabled`                      | `true` or `false`, enables the admin console. Default is `false`                                                                          |
| `outbound-connector.admin.console.login.mode`                   | `basic` or `keycloak`, enables basic or keycloak authentication. Authentication is disabled by default.                                   |
| `outbound-connector.admin.console.login.username`               | Only required if basic login is enabled. Username for the admin user.                                                                     |
| `outbound-connector.admin.console.login.encoded-password`       | Only required if basic login is enabled. Password for the admin user **_encrypted using BCrypt_**.                                        |
| `outbound-connector.admin.console.login.keycloak.client-id`     | Only required if Keycloak login is enabled. ID of the Keycloak client.                                                                    |
| `outbound-connector.admin.console.login.keycloak.client-secret` | Only required if Keycloak login is enabled. Client secret of the Keycloak client.                                                         |
| `outbound-connector.admin.console.login.keycloak.issuer-uri`    | Only required if Keycloak login is enabled. Sets the issuer URL of the Keycloak server, for example `http://localhost:8888/realms/EDDIE`. |

`eddie.public.url` and `eddie.management.url` are also required for the admin console to work as expected.

The region connector can be configured using Spring properties or environment variables.
When using environment variables, the configuration values need to be converted in the following way:

- Replace all non-alphanumeric characters with an underscore (`_`)
- Optionally convert all letters to upper case

For basic authentication:

```properties :spring
outbound-connector.admin.console.enabled=true
outbound-connector.admin.console.login.mode=basic
outbound-connector.admin.console.login.username=admin
outbound-connector.admin.console.login.encoded-password=$2a$10$qYTmwhGa3dd7Sl1CdXKKHOfmf0lNXL3L2k4CVhhm3CfY131hrcEyS # encrypted value of 'password'
```

For Keycloak authentication:

```properties :spring
outbound-connector.admin.console.enabled=true
outbound-connector.admin.console.login.mode=keycloak
outbound-connector.admin.console.login.keycloak.client-id=eddie-client
outbound-connector.admin.console.login.keycloak.client-secret=client-secret
outbound-connector.admin.console.login.keycloak.issuer-uri=http://localhost:8888/realms/EDDIE
```
