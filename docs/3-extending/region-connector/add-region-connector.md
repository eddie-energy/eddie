---
prev:
  text: "Tech Stack"
  link: "../tech-stack.md"
next:
  text: "Quickstart"
  link: "./quickstart.md"
---

# Add a region connector

region connectors are an essential part of EDDIE.
Their purpose is to create and manage [permission request](../../2-integrating/integrating.md#permission-requests) for the eligible party.
They connect to an MDA and PA, and in some cases, multiple regions at the same time can be supported by a single implementation if they implement the same processes and protocols.
For example, the [Green Button region-connector](../../1-running/region-connectors/region-connector-us-green-button.md) supports both the US and Canada, since they both have MDAs and PAs that implement the Green Button.
Region connectors have to process permission requests in a specific way, according to [the permission process model](../../2-integrating/integrating.md#permission-process-model).
They have to emit specific [messages](../../2-integrating/messages/connection-status-messages.md) and [documents](../../2-integrating/messages/permission-market-documents.md), when processing them.

How a permission request has to be processed is described in [implementing the permission process model](./internal-architecture.md#implementing-the-events).
A region connector has to follow a few conventions, but its internal architecture is up to the implementing party.
Keep in mind that there are best practices for region connectors and their [internal architecture](./internal-architecture.md).

A region connector is a Spring Boot application.
Each region connector is executed in its own Spring Boot context, which is registered as a child context of the EDDIE Core context.
EDDIE Core is the main module that initializes region and outbound connectors.
The Core scans the classpath at the base package
`energy.eddie.regionconnector` for region connectors and starts them if they are enabled.

Each region connector module has to have a class annotated with `@SpringBootApplication` at its base.
Spring Boot uses this base class to initialize the Spring context.
The following is a minimal example of this base class.

```java
package energy.eddie.regionconnector.foo.bar;

import energy.eddie.api.agnostic.RegionConnector;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RegionConnector(name = "foo-bar")
public class FluviusSpringConfig {
}
```

In total, only two annotations are needed to declare a region connector.
The [
`@RegionConnector`](./api.md#regionconnector-annotation) annotation is needed by EDDIE Core to find the region connector during classpath scanning.
The `@SpringBootApplication` to initialize the Spring context.
Other Spring annotations can also be added to the base class, such as `@EnableScheduling` or `@EnableWebMvc`.
Each region connector has its own [dispatcher servlet](./dispatcher-servlet.md), for web requests.

The EDDIE button will load the [region connector custom element](./frontend.md), when the correct region and permission administrator is selected.
The custom element must be implemented by each region connector.
It allows the final customer to give necessary information to create a permission request.

Since there are many parts to be implemented when creating a new region connector, here is a checklist of things that have to be considered and implemented.
This checklist is also present when creating a new [GitHub issue](https://github.com/eddie-energy/eddie/issues/new?assignees=&labels=needs-approval%2Crequirement&projects=&template=region-connector.md&title=%3Ccountry%3E+%3Cname+of+datahub%2Fpermission+admin%2Fmetered+data+admin%3E+region-connector) for implementing a region connector, so you can track everything in there too.
[Here the template itself.](https://github.com/eddie-energy/.github/blob/main/.github/ISSUE_TEMPLATE/region-connector.md)

<!-- @include: ../../parts/region-connector-checklist.md -->
