FROM eclipse-temurin:20-jre-alpine
COPY ./build/install/ /opt/
WORKDIR /opt/ep-demo-app
CMD ["sh", "./bin/ep-demo-app"]
EXPOSE 8080
