# Build the JAR file using Gradle in advance by running the bootJar task located at: eddie/aiida/Tasks/build/bootJar

FROM eclipse-temurin:21-jre-alpine
RUN mkdir -p /opt/security /opt/mqtt-persistence
RUN chown nobody:nobody /opt/security /opt/mqtt-persistence
COPY --chown=nobody:nobody ./build/libs/aiida-1.0.0.jar /opt/aiida.jar
WORKDIR /opt
USER nobody
HEALTHCHECK CMD wget --no-verbose -O /dev/null http://localhost:8080/actuator/health/ping || exit 1
CMD ["java", "-jar", "aiida.jar"]
EXPOSE 8080