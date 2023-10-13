FROM eclipse-temurin:20-jre-alpine
COPY ./build/install/ /opt/
WORKDIR /opt/eddie
CMD ["sh", "./bin/eddie"]
EXPOSE 8080
