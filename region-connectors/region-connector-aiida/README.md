# Region Connector for AIIDA

This README will guide you through the process of configuring the region connector for AIIDA, enabling near real-time
data receiving.

## Prerequisites

### Configuration of the Region Connector

| Configuration values                                    | Description                                                                                                                                                                                                                                                                                                                               |
|---------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `region-connector.aiida.service_name`                   | Name of the service how it will be displayed to the customer in their app/on AIIDA.                                                                                                                                                                                                                                                       |
| `region-connector.aiida.kafka.bootstrap_servers`        | List of Kafka bootstrap servers, to which AIIDA should send the near real-time data and status messages.                                                                                                                                                                                                                                  |
| `region-connector.aiida.kafka.data_topic`               | Kafka topic name, where to all AIIDA instances should stream their near real-time data.<br/>Make sure to only use characters allowed for Kafka topic names.                                                                                                                                                                               |
| `region-connector.aiida.kafka.status_messages_topic`    | Kafka topic name, where to all AIIDA instances should send ConnectionStatusMessages.<br/>Make sure to only use characters allowed for Kafka topic names.                                                                                                                                                                                  |
| `region-connector.aiida.kafka.termination_topic_prefix` | To avoid unnecessary traffic and for better security, each AIIDA instance gets a custom termination topic. The topic is created by concatenating this prefix and the connectionId of the permission with an underscore (_). Make sure that the prefix and the connectionId only contain characters that are valid for a Kafka topic name. |               

### .properties file

Example configuration for an `application.properties` file:

```properties
region-connector.aiida.service_name=My super cool test service
region-connector.aiida.kafka.bootstrap_servers=localhost:9093
region-connector.aiida.kafka.data_topic=aiida_data
region-connector.aiida.kafka.status_messages_topic=aiida_status_messages
region-connector.aiida.kafka.termination_topic_prefix=aiida_termination
```

### Environment variables

When using environment variables, the configuration values need to be converted in the following way:

* Replace all non-alphanumeric characters with an underscore (`_`)
* Optionally convert all letters to upper case

Example configuration for dotenv file:

```dotenv
REGION_CONNECTOR_AIIDA_SERVICE_NAME=My super cool test service
REGION_CONNECTOR_AIIDA_KAFKA_BOOTSTRAP_SERVERS=localhost:9093
REGION_CONNECTOR_AIIDA_KAFKA_DATA_TOPIC=aiida_data
REGION_CONNECTOR_AIIDA_KAFKA_STATUS_MESSAGES_TOPIC=aiida_status_messages
REGION_CONNECTOR_AIIDA_KAFKA_TERMINATION_TOPIC_PREFIX=aiida_termination
```

## Running the Region Connector via EDDIE

If you are using EDDIE, the region connector should appear in the list of available
region connectors if it has been configured correctly.