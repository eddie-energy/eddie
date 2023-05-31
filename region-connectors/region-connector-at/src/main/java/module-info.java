module energy.eddie.regionconnector.at {
    requires jakarta.annotation;
    requires jakarta.xml.bind;
    requires org.apache.commons.codec;

    exports energy.eddie.regionconnector.at.eda.requests;
    exports energy.eddie.regionconnector.at.eda.requests.restricted.enums;
    exports energy.eddie.regionconnector.at.eda.config;
    exports at.ebutilities.schemata.customerconsent.cmrequest._01p10;
}