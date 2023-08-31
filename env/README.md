In order to start the application in docker containers you will need to perform the following steps:

1. Build the application by running `./gradlew clean installDist` in the root directory of the project.
2. Build the docker images by running `docker-compose build` in the `/env` directory.
3. Configure the environment variables in the .env file
    * RegionConnectors:
        * add the needed configuration for the region connectors you want to start. Sample config can be found in the
          specific region connector example config files.
    * Kafka:
        * If you run Kafka via docker, you will need to provide the Gateway's IP address. This can be found by
          inspecting
          the container running the Kafka broker. For example by running `docker container ls` to get the id of the
          container and then inspecting the container with `docker inspect <container_id> | grep Gateway`.

4. Start the application by running `docker-compose up -d` in the root directory of the project.
5. Access the application via `http://localhost:9000/prototype/main/` in your browser.

If `.env` is configured correctly, the application should start, and you should be able to use all configured region
connectors.