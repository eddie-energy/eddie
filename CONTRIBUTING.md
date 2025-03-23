---
TODO:
  - Write and link dev button page
  - Document and link code quality standards
  - Automate formatting on push, so we do not have to document it in the PR guidelines
  - Merge in issue tracking (features, bugs) from https://github.com/eddie-energy/eddie/wiki/EDDIE-Development-&-Deployment-Strategy

Inspiration:
  - https://shoelace.style/resources/contributing
  - https://github.com/lit/lit/blob/main/CONTRIBUTING.md
  - https://github.com/withastro/astro/blob/main/CONTRIBUTING.md
---

# Contributing

This guide aims to help you set up your development environment and contribute to the EDDIE Framework.

## Prerequisites

The EDDIE Framework requires at least the following software:

- [JDK 21](https://www.jetbrains.com/help/idea/sdk.html#set-up-jdk)
- [Docker](https://www.docker.com/)

While our tooling does not depend on a specific IDE, we recommend using [IntelliJ IDEA](https://www.jetbrains.com/idea/) for a consistent developer experience.

### Web development

Gradle will automatically install the required Node.js and pnpm versions to build all frontend applications.

If you are actively working on frontend applications, you will also want to install these dependencies locally:

- [Node.js (18.17.0)](https://nodejs.org/en/download)
- [pnpm (8.5.0)](https://pnpm.io/pnpm-cli)

> [!TIP]
> You can use a tool like [nvm](https://github.com/nvm-sh/nvm) or [Volta](https://volta.sh/) to help manage multiple Node.js versions on your machine.

To make tooling like Prettier available to the IDE, their packages have to be installed at least once:

```shell
pnpm install
```

## Running

The EDDIE Framework requires at least a database to run, and you might want to set up additional services depending on your use-case.
The simplest way to set up such services is to use their Docker configuration in `env/docker-compose.yml`.

```shell
docker compose -f ./env/docker-compose.yml up -d db kafka
```

You can then run the EDDIE Framework using Gradle:

```shell
./gradlew run-core --args=--spring.profiles.active=dev,local
```

The EDDIE Core will run a demo page on http://localhost:8080/demo, which can be used to test the application with a configurable EDDIE button.

The repository includes IntelliJ run configurations for both the EDDIE Core and AIIDA.

- `Aiida (Gradle)`
- `Core (Gradle)`

The Spring configurations are better suited for general development and debugging.
However, they do not build external dependencies such as web resources.

- `Aiida (Spring)`
- `Core (Spring)`

All run configurations enable the `dev` and `local` Spring profiles to load `application-dev.properties` and `application-local.properties` respectively.
The `dev` profile configures the application for development, and `local` profiles are ignored by Git and ideal for your local configuration.

## Configuration & Testing

The [operation manual](https://eddie-web.projekte.fh-hagenberg.at/framework/1-running/OPERATION.html#configuration) includes detailed instructions on how to configure the EDDIE Framework.

If you want to test a specific regional implementation, you will need to enable and configure its region connector. This usually includes the configuration of secrets and access to test accounts. As a developer on the EDDIE team, you will have access to many configurations through our [Vaultwarden](https://vaultwarden-eddie.projekte.fh-hagenberg.at).

## Documentation

The documentation of EDDIE projects is split into architecture, project, and API documentation.

- [Architecture](https://eddie-web.projekte.fh-hagenberg.at/architecture/) documents abstract architectural concepts across EDDIE projects.
- [Framework](https://eddie-web.projekte.fh-hagenberg.at/framework/) documents how to operate, extend, and contribute to the EDDIE Framework.
- [Javadoc](https://eddie-web.projekte.fh-hagenberg.at/javadoc/) documents Java classes of the EDDIE Framework and is generated on code changes.

## Code Style

### Static Analysis

We are using SonarQube to analyze or code in the IDE and on pull requests.
When first opening the project, your IDE should ask you to set up the SonarQube plugin.
If you are not sure if SonarQube was installed,
please follow the [instructions](https://docs.sonarsource.com/sonarqube-for-ide/intellij/getting-started/installation/) for your IDE.

### Formatting

Most source code is formatted using IntelliJ's default formatting options,
with web languages using [Prettier](https://prettier.io/).
Configurations for both should be applied automatically from the repository.

## Pull Requests

We **merge** commits **into** the **main** branch when pushing changes and **rebase onto main** when pulling changes from it.

If you are using Git from the command line, you can set rebase as the default approach with

```shell
git config pull.rebase true
```

Please rebase onto main if you run into merge conflicts.
Do not push merge commits!

### Reviews

You need at least one review from a code owner for merging.

Making your pull request more accessible greatly reduces the time needed to merge it.
You can help reviewers by adding details on your changes in the ticket description.
If you are adding visual changes, please add before and after screenshots.

Please consider the following guidelines:

- All checks (GitHub Actions) should pass. This includes tests, linters, and code coverage. If any checks fail, you are responsible for fixing them. Please note that most code owners will not look at your pull request unless all checks succeeded.
  - Test, format, lint, and analyze your code before opening a pull request
- You are responsible for implementing feedback on your pull request
- You assign tickets to yourself if you want to work on them and update their project status
- Once your pull request is approved, you are responsible for merging it
- If your changes require documentation, please add it in the same pull request

Reviewers will assume that you followed these guidelines and that you actually tried your implementation.

### Commits

We do not squash commits when merging pull requests, so your commit messages matter!

The commit message should include the ticket number associated with it: `GH-123 Add this feature`.
The `GH-` prefix is short for GitHub and is used by IntelliJ to hyperlink ticket numbers.
This is done to later check why a change was implemented.
You should always reference a ticket in your commits. If no ticket exists, you want ot create one.
If it does not make sense to create a new ticket for trivial changes, NOISSUE can be used instead (e.g., `NOISSUE Fix typo in README`).
This should be used sparingly, as it makes it harder to understand why a change was made.
Take a look at the commit history as a point of reference.

### Branches

As a developer on the EDDIE team, you will usually push your feature branches to the upstream repository.
There is no enforced rule on how you should name your remote branches.
However, we recommend grouping them under your name, and optionally ticket number, for organization.
You can use your GitHub username or a short form of your real name.

For example:

- `re1/1187-development-guide`
- `arde/1187/development-guide` (for Arthur Dent)

## Developer Experience

We aim to make contributions and development as pleasant as possible.
Feel free to raise an issue if you feel like anything is missing from this guide or our documentation, or if you have an idea for improving the developer experience.
