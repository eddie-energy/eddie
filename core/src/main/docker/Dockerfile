FROM eclipse-temurin:21-jre-alpine
COPY ./build/install/ /opt/
WORKDIR /opt/core
CMD ["sh", "./bin/core"]
EXPOSE 8080
