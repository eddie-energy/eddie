# Build the JAR file using Gradle in advance by running the bootJar task located at: eddie/aiida/Tasks/build/bootJar

FROM eclipse-temurin:21-jre-alpine
COPY ./build/libs/aiida-1.0.0.jar /opt/aiida.jar
WORKDIR /opt
CMD ["java", "-jar", "aiida.jar"]
EXPOSE 8080