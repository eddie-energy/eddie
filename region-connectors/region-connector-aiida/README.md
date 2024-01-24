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
on the Kafka broker.

All messages are sent directly from AIIDA to the Kafka broker, nothing is routed through this
region connector (RC) and there is no direct communication between this RC and any AIIDA instance.

This RC also subscribes to the status message topic and updates the internal status of a permission when
such a message is received.

When a permission of this region connector should be terminated (_terminology: the EP requests a termination, the
customer revokes a permission_), this RC publishes a special message on the termination topic of the
specific permission. The AIIDA instance is subscribed to this topic and will therefore receive and honor the termination
request.

The topics for near real-time data and connection status messages are shared by **all** AIIDA instances.
Only the termination topic is unique for each permission.

## Prerequisites

### Configuration of the Region Connector

| Configuration values                                    | Description                                                                                                                                                                                                                                                                                                                                                                                        |
|---------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `region-connector.aiida.server.port=9988`               | Port on which the region-connector should listen.                                                                                                                                                                                                                                                                                                                                                  |
| `region-connector.aiida.kafka.bootstrap-servers`        | List of Kafka bootstrap servers, to which AIIDA instances should send the near real-time data and status messages. The region-connector also connects to this broker to listen for connection status messages. By default, the same value as specified for `kafka.bootstrap.servers` is used. Note that these brokers need to be accessible by the AIIDA instances (i.e. via the public internet). |
| `region-connector.aiida.kafka.data-topic`               | Kafka topic name, where to all AIIDA instances should stream their near real-time data.<br/>Make sure to only use characters allowed for Kafka topic names.                                                                                                                                                                                                                                        |
| `region-connector.aiida.kafka.status-messages-topic`    | Kafka topic name, where to all AIIDA instances should send ConnectionStatusMessages.<br/>Make sure to only use characters allowed for Kafka topic names.                                                                                                                                                                                                                                           |
| `region-connector.aiida.kafka.termination-topic-prefix` | To avoid unnecessary traffic and for better security, each AIIDA instance gets a custom termination topic. The topic is created by concatenating this prefix and the permissionID of the permission with an underscore (_). Make sure that the prefix only contains characters that are valid for a Kafka topic name.                                                                              |               
| `region-connector.aiida.kafka.group-id`                 | ID of the consumer group that the region-connector should be part of. Use `region-connector-aiida` unless a specific group-id is required.                                                                                                                                                                                                                                                         |
| `region-connector.aiida.customer.id`                    | A unique ID of the eligible party, should not be changed.                                                                                                                                                                                                                                                                                                                                          |

### .properties file

Example configuration for an `application.properties` file:

```properties
region-connector.aiida.server.port=9988
region-connector.aiida.kafka.bootstrap-servers=localhost:9092
region-connector.aiida.kafka.data-topic=aiida_data
region-connector.aiida.kafka.status-messages-topic=aiida_status_messages
region-connector.aiida.kafka.termination-topic-prefix=aiida_termination
region-connector.aiida.kafka.group-id=region-connector-aiida
region-connector.aiida.customer.id=my-unique-id
```

### Environment variables

When using environment variables, the configuration values need to be converted in the following way:

* Replace all non-alphanumeric characters with an underscore (`_`)
* Optionally convert all letters to upper case

Example configuration for dotenv file:

```dotenv
REGION_CONNECTOR_AIIDA_SERVER_PORT=9988
REGION_CONNECTOR_AIIDA_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
REGION_CONNECTOR_AIIDA_KAFKA_DATA_TOPIC=aiida_data
REGION_CONNECTOR_AIIDA_KAFKA_STATUS_MESSAGES_TOPIC=aiida_status_messages
REGION_CONNECTOR_AIIDA_KAFKA_TERMINATION_TOPIC_PREFIX=aiida_termination
REGION_CONNECTOR_AIIDA_KAFKA_GROUP_ID=region-connector-aiida
REGION_CONNECTOR_AIIDA_CUSTOMER_ID=my-unique-id
```

## Running the Region Connector via EDDIE

If you are using EDDIE, the region connector should appear in the list of available
region connectors if it has been configured correctly.
