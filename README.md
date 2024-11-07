# EDDIE - European Distributed Data Infrastructure for Energy

EDDIE provides a unified interface for requesting energy data from various MDAs (Metered Data Administrators) throughout
the European Union.
It abstracts the complex and diverse processes required by the different MDAs and provides easy access to energy data.

## Quick overview how EDDIE works

![EDDIE basic architecture](./docs/images/EDDIE%20Infrastructure.png "EDDIE basic architecture")

On your website you incorporate a so-called *EDDIE connect button*, which is loaded from the *EDDIE Core* and might look
like this:

![EDDIE connect button](./docs/images/EDDIE%20Connect%20Button.png "EDDIE connect button")

When the user clicks on the button, a popup opens:

![EDDIE popup](./docs/images/EDDIE%20Popup.png "EDDIE popup")

In the popup, the user has to select their country and their MDA. When they click on *Connect*, a *permission request*
for sharing their energy data is created.
These permission requests are handled by the *EDDIE Core*.
Any status messages (see
the [permission process](https://github.com/eddie-energy/eddie/wiki/EDDIE-Permission-Process-Model)) and data messages
(in various formats as described [here](docs/1-running/OPERATION.md)) are published by the core to a Kafka cluster. Your
website or application can then get these messages directly from the Kafka cluster (see used
topics [here](docs/2-integrating/KAFKA.md))

You can configure the data that is requested by creating a *Data Need* and passing its ID to the connect button.
For possible settings, see the *Data Needs* section, and the section *"Using the EDDIE Button in an application*
in [operation.md](docs/1-running/OPERATION.md).

## Running EDDIE with Docker

See the [operation.md](docs/1-running/OPERATION.md) file to get started.

## Running EDDIE locally

Firstly clone the repository.

Some modules provide custom web elements that need to be built with pnpm:

```bash
pnpm install
pnpm run build
```

### Gradle tasks

There are two tasks in the **development** group that have to run at the same time, e.g. in different windows:

- `./gradlew run-core`
- `./gradlew run-example-app`

The processes are:

#### Eligible party demo app

A simple demo app to check and try EDDIE's functionality.

- access the web-interface at <http://localhost:8081/login>
- login is possible with every email/password (not checked)

See [example app readme](docs/1-running/example-app.md) for further information.

#### EDDIE core

This doesn't offer a full-fledged web-interface.

- It's operation can be checked by accessing <http://localhost:8080/lib/eddie-components.js> which just delivers a JS
  file.

There are several parameters to configure the core via the environment:

- `JDBC_URL`: URL to the database
- `JDBC_USER`: Username for the database
- `JDBC_PASSWORD`: Password for the data
- `PUBLIC_CONTEXT_PATH`: Base path for reaching the application and the web components
- `CORE_PORT`: The port were the core should run

### Database & Kafka

Be aware that EDDIE Core needs a PostgreSQL database to function properly.
The example [docker-compose.yml](env/docker-compose.yml) contains a *db* service that you can use.

## Building docker images locally

Building and running can be done using `docker compose` with the files provided in the `/env` folder. For performing a
local test run with compiling the software, building and starting a local docker environment shell scripts exist:

- `build_and_run_containers.ps1`
- `build_and_run_containers.bash`

## Configuring EDDIE

Although the docker compose file and local configuration should run out of the box, EDDIE requires further
configuration.
Please read the *"Configuration*" section in the [operation.md](docs/1-running/OPERATION.md) file.

# Contributing

Development & Deployment Strategy can be
found [here](https://github.com/eddie-energy/eddie/wiki/EDDIE-Development-&-Deployment-Strategy).

To get started with the development process see [DEVELOPMENT.md](docs/3-extending/tech-stack.md).

To install, integrate and operate EDDIE, see the operation manual: [operation.md](docs/1-running/OPERATION.md).
