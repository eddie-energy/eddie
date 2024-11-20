# Outbound-connectors

Outbound-connectors allow the eligible party to interact with EDDIE via defined interfaces.
Which interfaces and protocols are supported depends on the implementation of the outbound-connector.
Currently, there are three implementations of the outbound-connectors:

- [admin-console](../admin-console.md): Allows interacting with EDDIE via a human-readable interface and is served as a webpage.
- [outbound-connector-kafka](../OPERATION.md#kafka-connector): Implements an outbound-connector that can interact with an Apache Kafka broker.
- [outbound-connector-amqp](outbound-connector-amqp.md): Implements an outbound-connector that can interact with an AMQP 1.0 compatible broker.