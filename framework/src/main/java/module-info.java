import energy.eddie.api.v0.RegionConnector;

module energy.eddie.framework {
    requires energy.eddie.api;
    requires io.javalin;
    requires org.eclipse.jetty.proxy;
    requires reactor.core;
    requires org.jdbi.v3.core;
    requires java.sql;
    requires com.google.guice;
    opens energy.eddie.framework;
    opens energy.eddie.framework.web;
    uses RegionConnector;

    // needed requires for runtime that aren't inferred
    requires kotlin.stdlib;
    requires org.eclipse.jetty.webapp;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires org.eclipse.jetty.websocket.jetty.server;
    requires energy.eddie.outbound.kafka;
}