import energy.eddie.api.v0.RegionConnector;
import energy.eddie.regionconnector.at.eda.EdaRegionConnector;

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

    // export region connector AT api to JAXB
    exports energy.eddie.regionconnector.at.api to com.fasterxml.jackson.databind;

    // open internal schemas for reflection by JAXB
    opens at.ebutilities.schemata.customerconsent.cmrequest._01p10 to jakarta.xml.bind;
    opens at.ebutilities.schemata.customerconsent.cmrevoke._01p00 to jakarta.xml.bind;
    opens at.ebutilities.schemata.customerconsent.cmnotification._01p11 to jakarta.xml.bind;
    opens at.ebutilities.schemata.customerprocesses.consumptionrecord._01p30 to jakarta.xml.bind;
    opens at.ebutilities.schemata.customerprocesses.masterdata._01p30 to jakarta.xml.bind;
    opens at.ebutilities.schemata.customerprocesses.common.types._01p20 to org.glassfish.jaxb.core, org.glassfish.jaxb.runtime;

    provides RegionConnector with EdaRegionConnector;
}