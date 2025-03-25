# Region Connector for AIIDA

This README will guide you through the process of configuring the region connector for AIIDA, enabling near real-time
data receiving.

## How does this region connector work?

AIIDA instances are run by customers in their homes, and they can share their in-house data, e.g. near real-time data
(1-15s) directly from the smart meter, with an eligible party (EP).

The customer visits the EP's website and clicks on the EDDIE connect button.
If the EP service requires near real-time data, the connect button sends a request to this region connector, requesting
a new permission. The region connector sends a response with the handshake information for AIIDA. See
the [E2E flow diagram](https://github.com/eddie-energy/eddie/blob/main/aiida/docs/diagrams/aiida_permission_e2e.plantuml) for more information in the handshake
and E2E flow. When the customer grants the permission, their AIIDA instance will send data and status messages to
separate topics
on the MQTT broker.

All messages are sent directly from AIIDA to the MQTT broker, nothing is routed through this
region connector (RC).

This RC also subscribes to the status message topic and updates the internal status of a permission when
such a message is received.

When a permission of this region connector should be terminated (_terminology: the EP requests a termination, the
customer revokes a permission_), this RC publishes a special message on the termination topic of the
specific permission. The AIIDA instance is subscribed to this topic and will therefore receive and honor the termination
request.

There is a dedicated topic per AIIDA instance and permission for the near real-time data and connection status messages.
ACLs and authentication ensure that only the permitted AIIDA instance may publish/subscribe to these topics.

## Prerequisites

### Configuration of the Region Connector

| Configuration values                     | Description                                                                                                                                                                                                                                                                                                                                                                                                                   |
|------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `region-connector.aiida.customer.id`     | A unique ID of the eligible party, should not be changed.                                                                                                                                                                                                                                                                                                                                                                     |
| `region-connector.aiida.bcrypt.strength` | Strength to be used by the BCryptPasswordEncoder instance used to hash the passwords for the MQTT user accounts for the AIIDA instances. It should be configured to a value that the hashing of a password takes around ~1 second. See also <a href="https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/crypto/bcrypt/BCrypt.html">Spring documentation for BCryptPasswordEncoder</a>. |
| `region-connector.aiida.mqtt.server.uri` | URI of the MQTT server which the AIIDA instances and the region connector use for communication.                                                                                                                                                                                                                                                                                                                              |
| `region-connector.aiida.mqtt.username`   | (Optional) Username to use when connecting to the MQTT broker (if not supplied, no username is used).                                                                                                                                                                                                                                                                                                                         |
| `region-connector.aiida.mqtt.password`   | (Optional) Password to use when connecting to the MQTT broker (if not supplied, no password is used).                                                                                                                                                                                                                                                                                                                         |

The region connector can be configured using Spring properties or environment variables.
When using environment variables, the configuration values need to be converted in the following way:

- Replace all non-alphanumeric characters with an underscore (`_`)
- Optionally convert all letters to upper case

```properties :spring
region-connector.aiida.customer.id=my-unique-id
region-connector.aiida.bcrypt.strength=14
region-connector.aiida.mqtt.server.uri=tcp://localhost:1883
region-connector.aiida.mqtt.username=testAccount
region-connector.aiida.mqtt.password=superSafe
```

## Running the Region Connector via EDDIE

If you are using EDDIE, the region connector should appear in the list of available
region connectors if it has been configured correctly.

## MQTT broker interface

The AIIDA region connector requires a MQTT broker to properly function. AIIDA instances will send their data messages to
this MQTT broker, and it is used as a means of communication between the AIIDA region connector and the AIIDA instances.
The region connector will create a user with a random password for each permission and the AIIDA instance can fetch
these credentials only once from the region connector. Access control lists (ACLs) for the user are created, to ensure
that the AIIDA instance may only publish/subscribe to authorized topics. The credentials of the user is stored in the
PostgreSQL database used by EDDIE core, in the `aiida` schema. The password is hashed and salted using BCrypt.

A dedicated user with only read access to the two tables of the ERM diagram shown below should be created for the MQTT
broker.

![AIIDA MQTT Broker.svg](images/AIIDA_MQTT_Broker.svg)

![MQTT Broker ERM.svg](images/MQTT_Broker_ERM.svg)

## EMQX MQTT broker

Please ensure, that you use a dedicated database user for the EMQX broker, and that you grant this user read permissions
to the `aiida.aiida_mqtt_user` and `aiida.aiida_mqtt_acl` tables.

```SQL
CREATE USER emqx WITH PASSWORD 'REPLACE_ME_WITH_SAFE_PASSWORD';
GRANT USAGE ON SCHEMA aiida TO emqx;
GRANT SELECT ON aiida.aiida_mqtt_acl TO emqx;
GRANT SELECT ON aiida.aiida_mqtt_user TO emqx;
GRANT CONNECT ON DATABASE eddie TO emqx;
```

EMQX MQTT broker supports authentication and authorization using PostgreSQL as backend, which should use a dedicated
user as seen above.
In order to add the EDDIE database for authentication and authorization, visit the dashboard of the broker
(by default running on port 18083).
Add the EDDIE database as source for authentication and authorization.

### Authentication

Select the **Authentication** tab, select **password-based** and then **PostgresDB**.
It is necessary to fill out the server address of the database, the database name and the credentials of the dedicated
user with which EMQX should connect.
For authentication, it is necessary to choose the **password hash** to be **bcrypt**.
Lastly, add the following SQL query to fetch the password hash for the given username.

```SQL
SELECT password_hash
FROM aiida.aiida_mqtt_user
WHERE username = ${username}
LIMIT 1;
```

### Authorization

Select the **Authorization** tab and select **PostgresDB**.
It is necessary to fill out the server address of the database, the database name and the credentials of the dedicated
user with which EMQX should connect.
Lastly, add the following SQL query to fetch the permissions for the given username.

```SQL
SELECT LOWER(action) AS action, LOWER(acl_type) AS permission, topic
FROM aiida.aiida_mqtt_acl
WHERE username = ${username};
```

Note that while enums are often used in uppercase, EMQX requires the `action` and `acl_type` to be in lowercase and
named `action` and `permission` respectively.

### Troubleshooting

When the EDDIE framework tries to connect to the MQTT broker anonymously, it is possible that the connection is refused.
To prevent this, it is possible to create a dedicated EMQX user for the EDDIE framework, which is used for the connection.
You can create the user by visiting the EMQX dashboard. Select the **Authentication** tab, select **password-based** and
then **Built-in Database
**. Here it is now possible to create user which can then be used by the AIIDA Region Connector by
adding it to the `region-connector.aiida.mqtt.username` and `region-connector.aiida.mqtt.password` configuration values.