# EDDIE - European Distributed Data Infrastructure for Energy

Development & Deployment Strategy can be
found [here](https://github.com/eddie-energy/eddie/wiki/EDDIE-Development-&-Deployment-Strategy)

To get started with the development process see [DEVELOPMENT.md](./DEVELOPMENT.md)

## How to run

Some modules provide custom web elements that need to be built with pnpm:

```bash
pnpm install
pnpm run build
```

There are three tasks in the **development** group that have to run at the same time, e.g. in different windows:

- `./gradlew run-db-server`
- `./gradlew run-framework`
- `./gradlew run-example-app`

The three processes are:

#### H2 Database Engine

[H2 Database Engine](https://www.h2database.com/html/main.html) started as a network server.

- the databases interface is acessible through <http://localhost:8082>
- JDBC connection through: `jdbc:h2:tcp://localhost/./examples/example-app` (username and password empty)

#### Eligible party demo app

A simple demo app to check the EDDIE Framework's functionality.

- access the web-interface through <http://localhost:8081/login>
- login is possible through every email/password (not checked)

#### EDDIE framework

This doesn't offer a full-fledged web-interface.

- It's operation can be checked by accessing <http://localhost:8080/lib/eddie-components.js> which just delivers a JS
  file.

## How to build docker images locally

Building and running can be done using `docker compose` with the files provided in the `/env` folder. For performing a
local test run with compiling the software, building and starting a local docker environment a powershell script
is provided: `build_and_run_containers.ps1`
