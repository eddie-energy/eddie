# Ponton folder

This folder is needed for the `EdaRegionConnector` to store the authentication token (`id.dat`) and other data
needed to establish a connection to a running `Ponton XP Messenger` instance.

If you want to use a different folder, you need to change the mount point in the `docker-compose.yml` file.

If you already have an authentication token, you can copy it to this folder and provide the
associated `AdapterId`via the `REGION_CONNECTOR_AT_EDA_PONTON_MESSENGER_ADAPTER_ID` environment variable via the `.env`
file.

