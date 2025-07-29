FROM ghcr.io/eddie-energy/eddie:latest
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar /opt/opentelemetry-javaagent.jar
WORKDIR /opt/core
ENV JAVA_TOOL_OPTIONS=-javaagent:/opt/opentelemetry-javaagent.jar
ENV OTEL_SERVICE_NAME=eddie
CMD ["sh", "./bin/core"]
EXPOSE 8080
