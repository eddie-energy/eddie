FROM eclipse-temurin:20-jre-alpine
COPY ./build/install/ /opt/
WORKDIR /opt/example-app
CMD ["sh", "./bin/example-app"]
EXPOSE 8080
