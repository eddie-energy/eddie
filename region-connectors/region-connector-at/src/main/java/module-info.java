module energy.eddie.regionconnector.at {
    requires jakarta.annotation;
    requires jakarta.xml.bind;
    requires org.apache.commons.codec;

    exports at.eda.requests;
    exports at.eda.requests.restricted.enums;
    exports at.eda.config;
    exports at.ebutilities.schemata.customerconsent.cmrequest._01p10;
}