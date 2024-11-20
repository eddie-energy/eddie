# Example app

A simple demo app to check and try EDDIE's functionality.

By default, you can access the web-interface at http://localhost:8081/login

Login is possible with every email/password combination (not checked).

## Configuration

The following environment variables can be configured for the example app:

| Parameter               | Description                                                                                                                                  |
|-------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| JDBC_URL                | PostgreSQL database where the example app can store data. DB server can be shared with EDDIE but use a separate database.                    |
| JDBC_USER               | Username to authenticate with the PostgreSQL server.                                                                                         |
| JDBC_PASSWORD           | Password to authenticate with the PostgreSQL server.                                                                                         |
| EDDIE_PUBLIC_URL        | Base URL of the EDDIE core.                                                                                                                  |
| PUBLIC_CONTEXT_PATH     | Base path for reaching the example application and the web components.                                                                       |
| KAFKA_BOOTSTRAP_SERVERS | Comma separated list of Kafka server IPs/hostnames.                                                                                          |
| EXAMPLE_APP_KAFKA_*     | (optional) All Environments starting with EXAMPLE_APP_KAFKA will be used to build the Kafka Configuration e.g. EXAMPLE_APP_SECURITY_PROTOCOL |
