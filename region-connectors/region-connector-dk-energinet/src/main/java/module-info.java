module energy.eddie.regionconnector.dk.energinet {
    requires energy.eddie.api;
    requires jakarta.annotation;
    requires com.fasterxml.jackson.annotation;
    requires org.openapitools.jackson.nullable;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires org.slf4j;
    requires org.apache.logging.log4j;
    requires kotlin.stdlib;

    requires eclipse.microprofile.config.api;
    requires java.net.http;
    requires feign.core;
    requires java.logging;
    requires feign.okhttp;
    requires feign.jackson;
    requires feign.slf4j;
    requires feign.form;

    exports energy.eddie.regionconnector.dk.energinet.customer.model;
    opens energy.eddie.regionconnector.dk.energinet.customer.model;
}