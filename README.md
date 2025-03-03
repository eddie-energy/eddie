# [![EDDIE - European Distributed Data Infrastructure for Energy](docs/images/eddie-horizontal.svg)](https://eddie.energy)

[EDDIE](https://eddie.energy) provides a unified interface for requesting energy data from various MDAs (Metered Data Administrators) throughout
the European Union.
It abstracts the complex and diverse processes required by the different MDAs and provides easy access to energy data.

This repository is home to both the EDDIE Framework and AIIDA.
AIIDA lives in the `aiida` folder and has its own [README](aiida/README.md).

## Getting started

The EDDIE Framework [documentation](https://eddie-web.projekte.fh-hagenberg.at/framework/) is a great place to start for both operators and developers.
It provides different entry points for:

- [Running the EDDIE Framework](https://eddie-web.projekte.fh-hagenberg.at/framework/1-running/OPERATION.html)
- [Integrating EDDIE into your application](https://eddie-web.projekte.fh-hagenberg.at/framework/2-integrating/integrating.html)
- [Extending or contributing to this repository](https://eddie-web.projekte.fh-hagenberg.at/framework/3-extending/tech-stack.html)

## A quick overview of how EDDIE works

![Simple diagram showing how the EDDIE Framework works](/docs/images/eddie-simple.svg)

The entry point on your website is the _EDDIE Button_.

![Visual representation of the EDDIE button](/docs/images/eddie-button.svg)

This button is configured with a [data need](https://eddie-web.projekte.fh-hagenberg.at/framework/2-integrating/data-needs.html) describing the data request.
When the customer clicks the button, a dialog will guide them through the permission process.
The customer will usually select their country and data administrator, and input additional data depending on their regional implementation.

![Showcase of the EDDIE dialog](/docs/images/eddie-dialog.webp)

Any [status messages](https://eddie-web.projekte.fh-hagenberg.at/framework/2-integrating/messages/permission-market-documents.html) as well as the [requested data](https://eddie-web.projekte.fh-hagenberg.at/framework/2-integrating/messages/validated-historical-data-market-documents.html) are published to configured [data sinks](https://eddie-web.projekte.fh-hagenberg.at/framework/1-running/outbound-connectors/outbound-connectors.html) (like [Kafka](https://eddie-web.projekte.fh-hagenberg.at/framework/1-running/outbound-connectors/outbound-connector-kafka.html)) to be retrieved by your application.

## Running EDDIE with Docker

The recommended way of running the EDDIE Framework is using [Docker](https://www.docker.com/) containers.
Instructions are found in the [operation manual](https://eddie-web.projekte.fh-hagenberg.at/framework/1-running/OPERATION.html).

## Running EDDIE locally

It is also possible to run the EDDIE Framework from source.

**Prerequisites:**

- Java Development Kit (JDK) Version 21 is installed
- Node.js Version 18 with pnpm

**Steps:**

1. Clone the repository
2. Start PostgreSQL and Apache Kafka: `docker compose -f .\env\docker-compose.yml up -d db kafka`
3. Edit core/src/main/resources/application.properties and add a (random) secret for signing JWTs, e.g.
   `eddie.jwt.hmac.secret=Y+nmICKhBcl4QbSItrf/IS9sVpUv4RMpiBtBPz0KYbM=`
4. Start EDDIE Framework using Gradle: `./gradlew run-core`
5. Start demo app (separate window): `./gradlew run-example-app`
6. Open browser on demo app: <http://localhost:8081/login>
   _login is possible with every email/password (not checked)_

See [example app readme](docs/1-running/example-app.md) for further information.

### Database & Kafka

Be aware that EDDIE Core needs a PostgreSQL database to function properly.
The example [docker-compose.yml](env/docker-compose.yml) contains the _db_ and _kafka_ services to be used.

## Building Docker images locally

Instructions on how to run the docker images locally exist in [env/README.md](env/README.md).
To perform a local test run with compiling the software, building, and starting a local docker environment, shell scripts exist in the repository root:

```shell
powershell build_and_run_containers.ps1
```

```shell
bash build_and_run_containers.bash
```

Although the docker compose file and local configuration should run out of the box, EDDIE requires further configuration.
Please refer to the [Configuration](https://eddie-web.projekte.fh-hagenberg.at/framework/1-running/OPERATION.html#configuration) of the operation manual.

## Contributing

Development & Deployment Strategy can be found [here](https://github.com/eddie-energy/eddie/wiki/EDDIE-Development-&-Deployment-Strategy).

To get started with the development process, see the [development guide](docs/3-extending/tech-stack.md).