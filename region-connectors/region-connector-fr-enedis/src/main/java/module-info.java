module energy.eddie.regionconnector.fr.enedis {
    requires energy.eddie.regionconnector.api;
    requires com.fasterxml.jackson.annotation;
    requires io.github.cdimascio.dotenv.java;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires java.net.http;
    requires org.apache.commons.codec;
    requires org.apache.httpcomponents.httpcore;
    requires org.apache.httpcomponents.httpclient;
    requires org.apache.httpcomponents.httpmime;
    requires jakarta.annotation;
    requires org.slf4j;
    requires org.apache.logging.log4j;
    requires org.openapitools.jackson.nullable;

    exports energy.eddie.regionconnector.fr.enedis;
    exports energy.eddie.regionconnector.fr.enedis.api;
    exports energy.eddie.regionconnector.fr.enedis.invoker;
    exports energy.eddie.regionconnector.fr.enedis.model;
}