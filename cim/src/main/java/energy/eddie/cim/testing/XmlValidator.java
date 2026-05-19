// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim.testing;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class XmlValidator {
    private static final Logger LOGGER = Logger.getLogger(XmlValidator.class.getName());

    private XmlValidator() {
        // Utility Class
    }

    // Only used during testing
    @SuppressWarnings({"CallToPrintStackTrace", "java:S4507", "java:S2755"})
    public static boolean validateXMLSchema(URL xsdPath, String xml) {
        try {
            var factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file");
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            var schema = factory.newSchema(xsdPath);
            var validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xml)));
        } catch (Exception e) {
            LOGGER.info(xml);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean validatePermissionMarketDocument(String xml) {
        var xsd = XmlValidator.class.getResource("/cim/xsd/v0_82/pmd/Permission_Envelope_2024-06-21T11.51.02.xsd");
        return validateXMLSchema(xsd, xml);
    }

    public static boolean validateAccountingPointMarketDocument(String xml) {
        var xsd = XmlValidator.class.getResource(
                "/cim/xsd/v0_82/ap/AccountingPoint_MarketDocument_2024-06-21T11.38.58.xsd");
        return validateXMLSchema(xsd, xml);
    }

    public static boolean validateValidatedHistoricalMarketDocument(String xml) {
        var xsd = XmlValidator.class.getResource(
                "/cim/xsd/v0_82/vhd/ValidatedHistoricalData_MarketDocument_2024-06-21T12.10.53.xsd"
        );
        return validateXMLSchema(xsd, xml);
    }

    public static boolean validateRtrEnvelope(String xml) {
        var xsd = XmlValidator.class.getResource(
                "/cim/xsd/v0_91_08/RedistributionTransactionRequest Document_Annotated.xsd"
        );
        return validateXMLSchema(xsd, xml);
    }

    public static boolean validateV104ValidatedHistoricalDataMarketDocument(byte[] xml) {
        var xsd = XmlValidator.class.getResource(
                "/cim/xsd/v1_04/vhd/ValidatedHistoricalData Document_v1.04_annotated.xsd");
        return validateXMLSchema(xsd, new String(xml, StandardCharsets.UTF_8));
    }

    public static boolean validateV112EnergySharingReferenceDataMarketDocument(byte[] xml) {
        var xsd = XmlValidator.class.getResource(
                "/cim/xsd/v1_12/esr/CEEDS_EnergySharingReferenceDataMarketDocument_annotated_v1.12.xsd");
        return validateXMLSchema(xsd, new String(xml, StandardCharsets.UTF_8));
    }

    public static boolean validateV112NearRealTimeDataMarketDocument(byte[] xml) {
        var xsd = XmlValidator.class.getResource(
                "/cim/xsd/v1_12/rtd/RealTimeData Document_v1.12_annotated.xsd");
        return validateXMLSchema(xsd, new String(xml, StandardCharsets.UTF_8));
    }

    public static boolean validateV112AcknowledgementMarketDocument(byte[] xml) {
        var xsd = XmlValidator.class.getResource(
                "/cim/xsd/v1_12/ack/Acknowledgement Document_v1.12_annotated.xsd");
        return validateXMLSchema(xsd, new String(xml, StandardCharsets.UTF_8));
    }

    public static boolean validateV112RequestPermissionMarketDocument(byte[] xml) {
        var xsd = XmlValidator.class.getResource(
                "/cim/xsd/v1_12/rpmd/RequestPermissionDocument_annotated_v1.12.xsd");
        return validateXMLSchema(xsd, new String(xml, StandardCharsets.UTF_8));
    }
}
