# E2E Tests

This module contains E2E tests for EDDIE and AIIDA.
These tests are not run by default when executing `./gradlew test`, but only when the `run-e2e-tests` property is present.

```sh
./gradlew e2e-tests:test -Prun-e2e-tests
```

For local execution, Playwright needs to be installed.
For Windows, macOS, Ubuntu, and Debian derivates, it should work out of the box, but for other Linux distributions the dependencies have to be installed manually.
On Arch Linux, Playwright can be installed from the AUR: `yay -S playwright`.

The tests default to http://localhost:8080 for EDDIE and http://localhost:8081 for AIIDA matching the default ports in the development setup.
EDDIE has to be running with the demo page enabled.
Ports can be changed by setting `E2E_EDDIE_URL`, `E2E_AIIDA_URL`, and `E2E_ADMIN_URL` environment variables.
`E2E_HEADLESS` can be set to `false` to run the tests in headed mode, which can be useful for debugging.

## Associated workflow file and GitHub secrets

The GitHub workflow file [run-playwright-e2e-tests.yml](../.github/workflows/run-playwright-e2e-tests.yml) is run whenever a new pull request is created or a merge into the main branch happens.
It fetches the Git repository, builds EDDIE and AIIDA images, runs their Docker Compose environments, and runs the Playwright E2E tests.
Necessary secrets have to be added to the repository on the GitHub website and are passed to the _eddie_ container (e.g. client secrets) and also to the Playwright tests (e.g. login credentials).

### Used secrets

Note that GitHub does not allow editing of existing secrets, but only overwriting. Therefore, when updating this
secret, always include the previously stored secrets as well, to avoid failing tests.

Binary secrets (e.g. certificates) have to be encoded in Base64 without newlines, and the output has to end with an equals symbol (=). Use `base64 -w 0 <file>` to generate the output.

- `E2E_TEST_ALL_SECRETS`: Contains all environment variables needed by the core and region connectors that hold potentially sensitive information (auth tokens, API keys). The contents are written to a `.env.local` file and passed to the `eddie` container. |
- `E2E_MIJN_AANSLUITING_JKS`: Binary contents of the key store file for the Mijn Aansluiting region connector.
- `E2E_FLUVIUS_CER`: Binary contents of the certificate file for the Fluvius region connector.
- `E2E_FLUVIUS_KEY`: Binary contents of the key store file for the Fluvius region connector.
- `PONTON_ID_DAT_FILE`: Binary contents of the `id.dat` file in Base64 encoding.

## Output

Playwright generates a test report in HTML format and retains [traces](https://playwright.dev/docs/trace-viewer) for failed tests.
Both are stored as an artifact in GitHub that can be downloaded after the workflow run completes.
The `trace.zip` files contain screenshots, logs, and a trace file that can be opened with the [Playwright Trace Viewer](https://trace.playwright.dev/) to analyze the test execution step by step.

## GitHub self-hosted runner

A self-hosted runner that is on the FH network is used so that the Ponton XP messenger can be accessed.
The self-hosted runner is started in a docker container, which shares its working directory as a volume with the host.
Currently, only one runner is available at the same time, therefore only 1 job can be processed simultaneously.
For a workflow to run on a self-hosted runner, the corresponding labels of the runner have to be specified in the workflow file, e.g. `runs-on: [ self-hosted, fh-server-e2e, dind ]`.
If one uses a label, that is used by a self-hosted as well as a GitHub-hosted runner, e.g. `ubuntu-22.04`, then GitHub will send the job to a self-hosted runner if one is available, and otherwise will use a GitHub-hosted one.

It would also be possible to not use a volume and make the runner completely ephemeral.
However, this is not feasible with our test setup, as the _eddie_ container needs secrets written to files to work properly.
These GitHub secrets are accessible in the workflow and written to a file to share with the _eddie_ container. However, the runner is also already running as a container so the files would have to be written to the host and then be mounted to the _eddie_ container.
The files have to be on the host, as the _eddie_ and other containers are started by the runner as sibling containers using the docker daemon of the host.
Therefore, all files/volumes that are mounted to newly started containers need to be accessible from the host.
Using a temporary volume seems a very brittle approach and therefore the current setup manually removes the whole work folder after a job completes, to ensure the next run has a clean work directory.

### Docker network

The runner and the containers for the E2E tests run in the same docker network, which allows the Playwright tests to directly access the containers without port mapping.
This avoids port clashes with other services running on the FH-server.

### Docker compose for GitHub self-hosted runner

```compose
services:
  e2e-runner:
    image: myoung34/github-runner:ubuntu-noble
    container_name: github-runner-e2e
    user: root
    environment:
      REPO_URL: https://github.com/eddie-energy/eddie
      RUNNER_NAME: fh-server-e2e
      ACCESS_TOKEN: REPLACE_ME_WITH_PERSONAL_ACCESS_TOKEN
      RUNNER_WORKDIR: /tmp/runner/work
      RUNNER_SCOPE: 'repo'
      LABELS: linux,x64,fh-server-e2e,dind
      ACTIONS_RUNNER_HOOK_JOB_COMPLETED: /home/admin/docker/github-runner-e2e/cleanup.sh
    security_opt:
      # needed on SELinux systems to allow docker container to manage other docker containers
      - label:disable
    volumes:
      - $PWD:$PWD
      - '/var/run/docker.sock:/var/run/docker.sock'
      - '/tmp/runner:/tmp/runner'
      # note: a quirk of docker-in-docker is that this path
      # needs to be the same path on host and inside the container,
      # docker mgmt cmds run outside of docker but expect the paths from within
    restart: "always"
    extra_hosts:
      - "host.docker.internal:host-gateway"

networks:
  default:
    name: "e2e-tests-network" # ensure this is the same network as in the docker-compose.yaml for the e2e tests!
```

See the [documentation](https://github.com/myoung34/docker-github-actions-runner) of the docker image for more details and a systemd example config.

#### cleanup.sh

Don't forget to `chmod +x cleanup.sh`

```
rm -rf $GITHUB_WORKSPACE
mkdir -p $GITHUB_WORKSPACE
```
