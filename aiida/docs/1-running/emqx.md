# EMQX

EMQX is a highly scalable and distributed MQTT broker.
It is used to connect data sources through the MQTT protocol.

AIIDA's EMQX broker is responsible for:

- Managing connections from multiple MQTT data sources
- Authenticating and authorizing these data sources
- Routing messages securely between publisher and subscriber

## Configuration
> [!NOTE]
> It is suggested to use the files provided in the [docker folder](https://github.com/eddie-energy/eddie/tree/main/aiida/docker) when using EMQX.

The [emqx folder](https://github.com/eddie-energy/eddie/tree/main/aiida/docker/emqx) contains the Infrastructure as Code (IaC) files for AIIDA's EMQX:

- [emqx.hocon](https://github.com/eddie-energy/eddie/blob/main/aiida/docker/emqx/emqx.hocon)
- [init-user.json](https://github.com/eddie-energy/eddie/blob/main/aiida/docker/emqx/init-user.json)
- [replace-password-with-env.sh](https://github.com/eddie-energy/eddie/blob/main/aiida/docker/emqx/replace-password-with-env.sh)

The main configuration is defined in the [emqx.hocon](https://github.com/eddie-energy/eddie/blob/main/aiida/docker/emqx/emqx.hocon) file.
This file contains the configuration of the authentication and authorization mechanisms.

The following values in the [.env](https://github.com/eddie-energy/eddie/blob/main/aiida/docker/.env) file adapt
these mechanisms:

| Environment Variable       | Description                                                                                                                                                                                                                                                                                                            |
|----------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| MQTT_PASSWORD              | The password for the `aiida` user in the built-in EMQX database. The `aiida` user is created with this password when EMQX is started for the first time. This user is used by AIIDA to connect to the EMQX broker.                                                                                                     |
| SPRING_DATASOURCE_DATABASE | The name of the database AIIDA and EMQX will connect to.                                                                                                                                                                                                                                                               |
| SPRING_DATASOURCE_HOST     | The hostname of the database server.                                                                                                                                                                                                                                                                                   |
| SPRING_DATASOURCE_PORT     | The port of the database server.                                                                                                                                                                                                                                                                                       |
| EMQX_DATABASE_PASSWORD     | The password for the `emqx` user in AIIDA's database. The `emqx` user is created with this password when the database is started for the first time (when using the [compose.yml](https://github.com/eddie-energy/eddie/blob/main/aiida/docker/compose.yml). This user is used by EMQX to connect to AIIDA's database. |

### Authentication

The [emqx.hocon](https://github.com/eddie-energy/eddie/blob/main/aiida/docker/emqx/emqx.hocon) file has two authentication
backends defined: `postgresql` and `built_in_database`.

The `postgresql` backend uses AIIDA's [database](database.md) to authenticate data sources.

The `built_in_database` backend on the other hand is stored in EMQX and is used to authenticate AIIDA itself.
To prefill the `built_in_database` backend the
[init-user.json](https://github.com/eddie-energy/eddie/blob/main/aiida/docker/emqx/init-user.json) and the
[replace-password-with-env.sh](https://github.com/eddie-energy/eddie/blob/main/aiida/docker/emqx/replace-password-with-env.sh)
are needed.

The [init-user.json](https://github.com/eddie-energy/eddie/blob/main/aiida/docker/emqx/init-user.json) defines the
`aiida` user. This user will be used by AIIDA to connect to the EMQX broker and is allowed to read from all the topics. 
The [replace-password-with-env.sh](https://github.com/eddie-energy/eddie/blob/main/aiida/docker/emqx/replace-password-with-env.sh)
replaces the password of the `aiida` user with the `MQTT_PASSWORD` environment variable of the
[.env](https://github.com/eddie-energy/eddie/blob/main/aiida/docker/.env) file.
In the [compose.yml](https://github.com/eddie-energy/eddie/blob/main/aiida/docker/compose.yml) file the `MQTT_PASSWORD`
is passed to EMQX as `AIIDA_MQTT_PASSWORD` and both the
[init-user.json](https://github.com/eddie-energy/eddie/blob/main/aiida/docker/emqx/init-user.json) and the
[replace-password-with-env.sh](https://github.com/eddie-energy/eddie/blob/main/aiida/docker/emqx/replace-password-with-env.sh)
are mounted in the container.

### Authorization

The [emqx.hocon](https://github.com/eddie-energy/eddie/blob/main/aiida/docker/emqx/emqx.hocon) file has `postgresql` as
authorization backend defined.
It uses AIIDA's [database](database.md) to check if a user is allowed to publish or subscribe to a certain topic.
It determines this by querying for the `topic` the user performed an action on, the `action` the user is allowed to perform
and the `acl_type` for this topic.

If no match was found, the user's action on this topic is denied.
If the broker denies an action of a user it ignores it and will not disconnect the user.

### Encryption
The MQTT connection between the data source and the EMQX broker can be secured with Transport Layer Security (TLS).
The MQTT connection between AIIDA and the EMQX broker is unsecured.