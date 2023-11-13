import energy.eddie.api.v0.RegionConnectorFactory;
import energy.eddie.regionconnector.aiida.AiidaRegionConnectorFactory;

module energy.eddie.regionconnector.aiida {
    requires jakarta.annotation;
    requires reactor.core;
    requires org.slf4j;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires energy.eddie.api;
    requires eclipse.microprofile.config.api;
    requires energy.eddie.region.connector.shared;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.web;
    requires spring.context;
    requires spring.core;
    requires spring.beans;
    requires spring.boot.starter.validation;
    requires jakarta.validation;
    requires org.jboss.logging;
    requires com.fasterxml.classmate;
    requires spring.webmvc;
    requires org.reactivestreams;

    exports energy.eddie.regionconnector.aiida;
    // Needed for spring
    exports energy.eddie.regionconnector.aiida.web;
    exports energy.eddie.regionconnector.aiida.services;
    exports energy.eddie.regionconnector.aiida.config;

    // Needed for spring
    opens energy.eddie.regionconnector.aiida to spring.core;

    provides RegionConnectorFactory with AiidaRegionConnectorFactory;
}