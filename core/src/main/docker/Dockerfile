FROM eclipse-temurin:21-jre-alpine
HEALTHCHECK CMD wget --no-verbose http://localhost:8080/actuator/health/ping || exit 1
COPY --chown=nobody:nobody ./build/install/ /opt/
WORKDIR /opt/core
USER nobody
CMD ["sh", "./bin/core"]
EXPOSE 8080
