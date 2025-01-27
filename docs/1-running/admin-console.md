# Administrative console

The admin console allows the eligible party to manage permission via a web interface.
It will be served via `<hostname>:<eddie.management.server.port>/outbound-connectors/admin-console`.
The default for `eddie.management.server.port` is `9090`.

The admin console can be configured to require authentication.
Currently, only basic auth and a single admin user is supported.
The username for the admin user and a ***BCrypt encrypted*** password can be configured using properties or environment variables, as described below.

## Configuration of the Admin Console

| Configuration values                                     | Description                                                                                                                                                     |
|----------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `outbound-connector.admin.console.enabled`               | `true` or `false`, enables the admin console. Default is `false`                                                                                                |
| `outbound-connector.admin.console.login.enabled`         | `true` or `false`, enables basic auth authentication. Default is `false`                                                                                        |
| `outbound-connector.admin.console.login.username`        | Only required if login is enabled. Username for the admin user.                                                                                                 |
| `outbound-connector.admin.console.login.encodedPassword` | Only required if login is enabled. Password for the admin user ***encrypted using BCrypt***.                                                                    |
| `outbound-connector.admin.console.url`                   | Full URL of the deployed admin console. Usually `<protocol>://<origin>:${eddie.management.server.port}/outbound-connectors/admin-console`.                      |
| `outbound-connector.admin.console.management.url`        | Full URL of the management API including its url prefix. Usually  `<protocol>://<origin>:${eddie.management.server.port}/${eddie.management.server.urlprefix}`. |

### .properties file

Example configuration for an `application.properties` file:

```properties
outbound-connector.admin.console.enabled=true
outbound-connector.admin.console.login.enabled=true
outbound-connector.admin.console.login.username=admin
outbound-connector.admin.console.login.encodedPassword=$2a$10$qYTmwhGa3dd7Sl1CdXKKHOfmf0lNXL3L2k4CVhhm3CfY131hrcEyS # encrypted value of 'password'
```

### Environment variables

When using environment variables, the configuration values need to be converted in the following way:

* Replace all non-alphanumeric characters with an underscore (`_`)
* Optionally convert all letters to upper case

Example configuration for dotenv file:

```dotenv
OUTBOUND_CONNECTOR_ADMIN_CONSOLE_ENABLED=true
OUTBOUND_CONNECTOR_ADMIN_CONSOLE_LOGIN_ENABLED=true
OUTBOUND_CONNECTOR_ADMIN_CONSOLE_LOGIN_USERNAME=admin
OUTBOUND_CONNECTOR_ADMIN_CONSOLE_LOGIN_ENCODEDPASSWORD=$2a$10$qYTmwhGa3dd7Sl1CdXKKHOfmf0lNXL3L2k4CVhhm3CfY131hrcEyS # encrypted value of 'password'
```
