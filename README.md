# EDDIE - European Distributed Data Infrastructure for Energy

Development & Deployment Strategy can be
found [here](https://github.com/eddie-energy/eddie/wiki/EDDIE-Development-&-Deployment-Strategy)

To get started with the development process see [DEVELOPMENT.md](./DEVELOPMENT.md)

To install, integrate and operate EDDIE, see the operation manual: [OPERATION.md](OPERATION.md)

## How to run in development

Some modules provide custom web elements that need to be built with pnpm:

```bash
pnpm install
pnpm run build
```

### Configuring different regions / countries

To retrieve data from various regions and countries, it is crucial to correctly set up the relevant region connectors.
These connectors can be configured through two methods: by modifying the `application.properties` file of
the `core` module or by utilizing environment variables.

For each region connector, specific configurations and prerequisites are necessary for operation. Details for these
setups are provided in the README file of the individual region connector. You can locate these files
under `region-connectors/region-connector-<country-code>-<permission-administrator>/README.md`.

Or you can use the following links:

* [Austria (EDA)](./region-connectors/region-connector-at-eda/README.md)
* [France (Enedis)](./region-connectors/region-connector-fr-enedis/README.md)
* [Spain (Datadis)](./region-connectors/region-connector-es-datadis/README.md)
* [Denmark (Energinet)](./region-connectors/region-connector-dk-energinet/README.md)

### Gradle tasks

There are three tasks in the **development** group that have to run at the same time, e.g. in different windows:

- `./gradlew run-db-server` (obsolete, the default DB is a PostgreSQL)
- `./gradlew run-core`
- `./gradlew run-example-app`

The three processes are:

#### Database

Instead of the H2 database, a PostgreSQL database is used as default.
Check the [docker-compose.yml](env/docker-compose.yml) file for an example PostgreSQL container that you can use as it
is. The necessary database for the example app and the EDDIE core are created automatically when starting the docker
container for the first time.

You can still use the H2 database by changing the JDBC URLs for the example app and the EDDIE core and running
the `run-db-server` gradle task.
[H2 Database Engine](https://www.h2database.com/html/main.html) started as a network server.

- the databases interface is accessible through <http://localhost:8082>
- JDBC connection through: `jdbc:h2:tcp://localhost/./examples/example-app` (username and password empty)

#### Eligible party demo app

A simple demo app to check EDDIE's functionality.

- access the web-interface through <http://localhost:8081/login>
- login is possible through every email/password (not checked)

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

## How to build docker images locally

Building and running can be done using `docker compose` with the files provided in the `/env` folder. For performing a
local test run with compiling the software, building and starting a local docker environment shell scripts exist:

- `build_and_run_containers.ps1`
- `build_and_run_containers.bash`