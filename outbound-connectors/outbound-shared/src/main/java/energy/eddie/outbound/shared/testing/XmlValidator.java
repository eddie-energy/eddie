package energy.eddie.outbound.shared.testing;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;

public class XmlValidator {

    private XmlValidator() {
        // Utility Class
    }

    public static boolean validateXMLSchema(URL xsdPath, String xml) {
        try {
            var factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            var schema = factory.newSchema(xsdPath);
            var validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xml)));
        } catch (IOException | SAXException e) {
            return false;
        }
        return true;
    }

    public static boolean validatePermissionMarketDocument(String xml) {
        var xsd = XmlValidator.class.getResource("/cim/xsd/v0_82/pmd/Permission_Enveloppe_2024-06-21T11.51.02.xsd");
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
}
