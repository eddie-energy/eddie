FROM eclipse-temurin:20-jre-alpine
COPY ./build/install/ /opt/
WORKDIR /opt/framework
CMD ["sh", "./bin/framework"]
EXPOSE 8080
