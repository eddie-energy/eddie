FROM eclipse-temurin:21-jre-alpine
COPY --chown=nobody:nobody ./build/install/ /opt/
WORKDIR /opt/example-app
USER nobody
HEALTHCHECK CMD wget --no-verbose http://localhost:8080/login || exit 1
CMD ["sh", "./bin/example-app"]
EXPOSE 8080
