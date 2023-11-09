import energy.eddie.api.v0.RegionConnectorFactory;
import energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorFactory;

module energy.eddie.regionconnector.dk.energinet {
    requires energy.eddie.api;
    requires jakarta.annotation;
    requires com.fasterxml.jackson.annotation;
    requires org.openapitools.jackson.nullable;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires org.slf4j;
    requires kotlin.stdlib;

    requires eclipse.microprofile.config.api;
    requires java.net.http;
    requires feign.core;
    requires java.logging;
    requires feign.okhttp;
    requires feign.jackson;
    requires feign.slf4j;
    requires feign.form;
    requires io.javalin;
    requires reactor.core;
    requires energy.eddie.region.connector.shared;
    requires spring.context;
    requires spring.boot;

    // Needed for spring
    opens energy.eddie.regionconnector.dk to spring.core;
    exports energy.eddie.regionconnector.dk;

    exports energy.eddie.regionconnector.dk.energinet.customer.model;
    opens energy.eddie.regionconnector.dk.energinet.customer.model;
    provides RegionConnectorFactory with EnerginetRegionConnectorFactory;
}