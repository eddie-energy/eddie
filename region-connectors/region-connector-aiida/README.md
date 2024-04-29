# Region Connector for AIIDA

This README will guide you through the process of configuring the region connector for AIIDA, enabling near real-time
data receiving.

## How does this region connector work?

AIIDA instances are run by customers in their homes, and they can share their in-house data, e.g. near real-time data
(1-15s)
directly from the smart meter, with an eligible party (EP).

The customer visits the EP's website and clicks on the EDDIE connect button.
If the EP service requires near real-time data, the connect button sends a request to this region connector, requesting
a new permission. The region connector sends a response with all the necessary information that AIIDA requires to start
the data sharing. This information is displayed to the customer, and they will enter it in AIIDA.
When the customer grants the permission, their AIIDA instance will send data and status messages to separate topics
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

| Configuration values                                    | Description                                                                                                                                                                                                                                                                                                                                                                                                                   |
|---------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `region-connector.aiida.customer.id`                    | A unique ID of the eligible party, should not be changed.                                                                                                                                                                                                                                                                                                                                                                     |
| `region-connector.aiida.bcrypt.strength`                | Strength to be used by the BCryptPasswordEncoder instance used to hash the passwords for the MQTT user accounts for the AIIDA instances. It should be configured to a value that the hashing of a password takes around ~1 second. See also <a href="https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/crypto/bcrypt/BCrypt.html">Spring documentation for BCryptPasswordEncoder</a>. |

### .properties file

Example configuration for an `application.properties` file:

```properties
region-connector.aiida.customer.id=my-unique-id
region-connector.aiida.bcrypt.strength=14
```

### Environment variables

When using environment variables, the configuration values need to be converted in the following way:

* Replace all non-alphanumeric characters with an underscore (`_`)
* Optionally convert all letters to upper case

Example configuration for dotenv file:

```dotenv
REGION_CONNECTOR_AIIDA_CUSTOMER_ID=my-unique-id
REGION_CONNECTOR_AIIDA_BCRYPT_STRENGTH=14
```

## Running the Region Connector via EDDIE

If you are using EDDIE, the region connector should appear in the list of available
region connectors if it has been configured correctly.
