FROM eclipse-temurin:21-jre-alpine
COPY ./build/libs/backend-0.0.1.jar /opt/example-app-backend.jar
COPY ./src/docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
WORKDIR /opt
CMD ["/entrypoint.sh"]
EXPOSE 8080
