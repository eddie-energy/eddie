module energy.eddie.regionconnector.at {
    requires jakarta.annotation;
    requires jakarta.xml.bind;
    requires org.apache.commons.codec;
    requires de.ponton.xp.adapterapi;
    requires reactor.core;
    requires org.slf4j;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires energy.eddie.api;
    requires io.javalin;
    requires kotlin.stdlib;


    exports energy.eddie.regionconnector.at.eda.requests;
    exports energy.eddie.regionconnector.at.eda.requests.restricted.enums;
    exports energy.eddie.regionconnector.at.eda.config;
    exports at.ebutilities.schemata.customerconsent.cmrequest._01p10;
}