FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
HEALTHCHECK CMD curl -f localhost:8080/actuator/health/ping || exit 1
COPY ./build/install/ /opt/
WORKDIR /opt/core
CMD ["sh", "./bin/core"]
EXPOSE 8080
