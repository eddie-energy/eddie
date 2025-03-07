---
prev:
  text: "Data Needs"
  link: "../2-integrating/data-needs.md"
next:
  text: "Add a region connector"
  link: "./region-connector/add-region-connector.md"
---

# Tech Stack

**Backend**

- Java (JDK 21)
  - Project Reactor (Reactivity)
  - SLF4J (Logging)
- Spring (Backends)
  - Hibernate
- Javalin (Minimal Java web framework for the example app)
- Gradle (Build tool)

**Frontend**

- pnpm (Package manager with workspaces)
- Vue (SPAs)
  - PrimeVue (Component library)
- Lit (Button & Web Components)
- Shoelace (Component library)
- VitePress (Documentation)

**DevOps**

- Docker (Containerization)
  - Compose (Multiple Containers)
- Kubernetes (Orchestration)
  - Helm (Deployment)
  - K3s (IOT/AIIDA)
- Playwright (E2E Tests)
- Keycloak (Authentication)
- Message Brokers (Outbound Connectors)
  - Kafka
  - EMQX/NanoMQ (MQTT)
  - RabbitMQ (AMQP)
- GitHub Actions (CI/CD)
