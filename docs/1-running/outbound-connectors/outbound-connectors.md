# Outbound-connectors

Outbound-connectors allow the eligible party to interact with EDDIE via defined interfaces.
Which interfaces and protocols are supported depends on the implementation of the outbound-connector.
Current implementations of the outbound-connectors are as follows:

- [admin-console](../admin-console.md): Allows interacting with EDDIE via a human-readable interface and is served as a webpage.
  It is considered a special outbound connector since it supports more features than a simple outbound connector.
- [outbound-connector-kafka](outbound-connector-kafka.md): Implements an outbound-connector that can interact with an Apache Kafka broker.
- [outbound-connector-amqp](outbound-connector-amqp.md): Implements an outbound-connector that can interact with an AMQP 1.0 compatible broker.
- [outbound-connector-rest](outbound-connector-rest.md): Implements an outbound-connector for REST clients.
- [outbound-connector-metric](outbound-connector-metric.md): Implements an outbound-connector for metrics.

These outbound connector implementations allow the eligible party to have a selection of different protocols.
This allows the EP to reuse already existing deployments of brokers and integrations.
Not all outbound connectors have to be active at the same time.