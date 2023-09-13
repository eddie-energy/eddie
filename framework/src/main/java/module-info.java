module energy.eddie.framework {
    requires energy.eddie.api;
    requires io.javalin;
    requires org.eclipse.jetty.proxy;
    requires reactor.core;
    requires org.jdbi.v3.core;
    requires java.sql;
    requires com.google.guice;
    requires eclipse.microprofile.config.api;
    requires jakarta.annotation;
    requires com.google.code.findbugs.jsr305;
    opens energy.eddie.framework;
    opens energy.eddie.framework.dataneeds;
    opens energy.eddie.framework.web;
    uses energy.eddie.api.v0.RegionConnector;
    uses energy.eddie.api.v0.RegionConnectorFactory;

    // needed requires for runtime that aren't inferred
    requires kotlin.stdlib;
    requires org.eclipse.jetty.webapp;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires org.eclipse.jetty.websocket.jetty.server;
    requires energy.eddie.outbound.kafka;
    requires io.smallrye.common.function;
    requires io.smallrye.common.classloader;
    requires io.smallrye.common.expression;
    requires io.smallrye.config.core;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.web;
    requires spring.context;
}