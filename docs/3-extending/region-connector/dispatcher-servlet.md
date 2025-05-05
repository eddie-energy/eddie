# Dispatcher Servlet (web interface)

Each region connector is started in its own spring context and runs in a separate dispatcher servlet.
This means that each region connector can serve data via HTTP to create new permission requests, handle OAUTH callbacks, or provide other information for the [user interface](./frontend.md).
The region connector is available under `<hostname>:<server.port>/region-connectors/<region-connector-ID>`.
The custom element is served under `<hostname>:<server.port>/region-connectors/<region-connector-ID>/ce.js`.

## Common REST Endpoints

There are some common endpoints that should be implemented by each region connector.
They are listed in [RestApiPaths](https://eddie-web.projekte.fh-hagenberg.at/javadoc/energy/eddie/regionconnector/shared/web/RestApiPaths.html).
Two always need to be implemented.
First, the `PATH_PERMISSION_REQUEST` endpoint to create permission requests.
Second, the `PATH_PERMISSION_STATUS_WITH_PATH_PARAM` to request the current status of a permission request.