FROM eclipse-temurin:20-jre-alpine
COPY ./build/install/ /opt/
WORKDIR /opt/eddie
CMD ["sh", "./bin/core"]
EXPOSE 8080
