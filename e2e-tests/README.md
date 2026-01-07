# E2E Tests

This module contains E2E tests for EDDIE using the example app.

## Associated workflow file and GitHub secrets

The GitHub workflow file [run-playwright-e2e-tests.yml](../.github/workflows/run-playwright-e2e-tests.yml) is run
whenever a new pull request is created or a merge into the main branch happens. It fetches the git repository, builds
and runs the docker images, and runs the Playwright E2E tests. Necessary secrets have to be added to the repository on
the GitHub website and are passed to the *core* container (e.g. client secrets) and also to the Playwright tests (e.g.
login credentials). Gradle test tasks are not run by the workflow, as some tests require testcontainers, and those need
to communicate with the docker gateway, which is blocked by our firewall. The port is also not always the same, so
adding a firewall allow rule doesn't work
either.

### Used secrets

- PONTON_ID_DAT_FILE: Contains the contents of the *id.dat* file in BASE64 encoding without newlines.
  Use `base64 -w 0 id.dat` to generate output and make sure, that the output ends with an equals symbol (=)
- E2E_TEST_ALL_SECRETS: Contains all secrets that are needed by *core* (e.g. client secrets for FR Enedis). The contents
  are written to a file called `.env.local` and passed to the *core* container. Separate multiple entries by newline.
  Note that GitHub does not allow editing of existing secrets, but only overwriting. Therefore, when updating this
  secret, always include the previously stored secrets as well, to avoid failing tests.
- DK_ENERGINET_METERING_POINT and DK_ENERGINET_REFRESH_TOKEN are passed directly to the playwright tests.

## Output

If any testcase fails, screenshots of the pages when the failure occurs (i.e. when an assertion failed), as well as the
whole test report as HTML file are uploaded as artifacts and attached to the workflow run to allow easy debugging.

## Run locally

To run the e2e tests locally, playwright needs to be installed.
For Windows, macOS, and Debian derivates, it should work out of the box, but for other Linux distributions the dependencies have to be installed manually.
On Arch Linux, Playwright can be installed from the AUR: `yay -S playwright`.

The tests are executed via the `test` task with the `run-e2e-tests` property present. The EDDIE core has to be running with the demo page enabled.

```sh
./gradlew -p ../ e2e-tests:test -Prun-e2e-tests
```

## GitHub self-hosted runner

A self-hosted runner that is on the FH network is used so that the Ponton XP messenger can be accessed.
The self-hosted runner is started in a docker container, which shares its working directory as a volume with the host.
Currently, only one runner is available at the same time, therefore only 1 job can be processed simultaneously.
For a workflow to run on a self-hosted runner, the corresponding labels of the runner have to be specified in the
workflow file, e.g.
`runs-on: [ self-hosted, fh-server, dind ]`. If one uses a label, that is used by a self-hosted as well as
a GitHub-hosted runner, e.g. `ubuntu-22.04`, then GitHub will send the job to a self-hosted runner if one is available,
and otherwise will use a GitHub-hosted one.

It would also be possible to not use a volume and make the runner completely ephemeral. However, this is not feasible
with our test setup, as the *eddie core* container needs secrets written to files to work properly. These GitHub secrets
are accessible in the workflow and can be written to a file, but as the runner is also already running as a container,
the files would have to be written to the host and then be mounted to the *core* container. The files have to be on the
host, as the *core* and other containers are started by the runner as sibling containers using the docker daemon
of the host, therefore all files/volumes that are mounted to newly started containers need to be accessible from the
host. Using a temporary volume seems a very brittle approach and therefore the current setup manually removes the whole
work folder after a job completes, to ensure the next run has a clean work directory.

### Docker network

The runner and the containers for the E2E tests run in the same docker network, which allows the playwright tests to
directly access the containers without port mapping. This avoids port clashes with other services running on the
FH-server.

### Docker compose for GitHub self-hosted runner

```
version: '2.3'
services:
  worker:
    image: myoung34/github-runner:latest
    environment:
      REPO_URL: https://github.com/eddie-energy/eddie
      RUNNER_NAME: fh-server-docker
      ACCESS_TOKEN: REPLACE_ME_WITH_PERSONAL_ACCESS_TOKEN
      RUNNER_WORKDIR: /tmp/runner/work
      RUNNER_SCOPE: 'repo'
      LABELS: linux,x64,fh-server
      ACTIONS_RUNNER_HOOK_JOB_COMPLETED: /home/admin/docker/github-runner/cleanup.sh
    security_opt:
      # needed on SELinux systems to allow docker container to manage other docker containers
      - label:disable
    volumes:
      - '/var/run/docker.sock:/var/run/docker.sock'
      - '/tmp/runner:/tmp/runner'
      # note: a quirk of docker-in-docker is that this path
      # needs to be the same path on host and inside the container,
      # docker mgmt cmds run outside of docker but expect the paths from within

networks:
  default:
    name: "e2e-tests-network" # ensure this is the same network as in the docker-compose.yaml for the e2e tests!
```

See the [documentation](https://github.com/myoung34/docker-github-actions-runner) of the docker image for more details
and e.g. a systemd example config.

#### cleanup.sh

Don't forget to `chmod +x cleanup.sh`

```
rm -rf $GITHUB_WORKSPACE
mkdir -p $GITHUB_WORKSPACE
```

Could use playwright image which has dependencies pre-installed but then need to mount again the source files, etc...
