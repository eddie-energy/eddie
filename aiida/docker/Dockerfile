# Build the JAR file using Gradle in advance by running the bootJar task located at: eddie/aiida/Tasks/build/bootJar

FROM eclipse-temurin:21-jre-ubi9-minimal

RUN echo "Setting environment variables"
ENV APP_USER=aiida
ENV APP_GROUP=eddie-energy
ENV APP_HOME=/usr/local/${APP_USER}

RUN groupadd --system "${APP_GROUP}" && \
    useradd --system --create-home --home-dir "${APP_HOME}" -g "${APP_GROUP}" "${APP_USER}"

RUN mkdir -p "${APP_HOME}"/security && \
    chown -R "${APP_USER}":"${APP_GROUP}" "${APP_HOME}"

COPY ./build/libs/aiida-1.0.0.jar ${APP_HOME}/aiida.jar

COPY ./docker/docker-entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

WORKDIR ${APP_HOME}

USER ${APP_USER}

EXPOSE 8080
ENTRYPOINT ["/entrypoint.sh"]
CMD ["java", "-jar", "aiida.jar"]