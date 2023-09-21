import energy.eddie.api.v0.RegionConnectorFactory;
import energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorFactory;

module energy.eddie.regionconnector.es {
    requires energy.eddie.api;
    requires reactor.core;
    requires io.javalin;
    requires org.slf4j;
    requires jakarta.annotation;
    requires com.fasterxml.jackson.annotation;
    requires eclipse.microprofile.config.api;
    requires com.fasterxml.jackson.databind;
    requires reactor.netty.http;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires io.netty.codec.http;
    requires energy.eddie.region.connector.shared;
    requires kotlin.stdlib;
    requires reactor.netty.core;

    exports energy.eddie.regionconnector.es.datadis;
    exports energy.eddie.regionconnector.es.datadis.serializer;
    exports energy.eddie.regionconnector.es.datadis.api;
    exports energy.eddie.regionconnector.es.datadis.client;
    exports energy.eddie.regionconnector.es.datadis.config;
    exports energy.eddie.regionconnector.es.datadis.dtos;
    exports energy.eddie.regionconnector.es.datadis.dtos.exceptions;
    exports energy.eddie.regionconnector.es.datadis.permission.request.api;
    opens energy.eddie.regionconnector.es.datadis.dtos to com.fasterxml.jackson.databind;

    provides RegionConnectorFactory with DatadisRegionConnectorFactory;
}