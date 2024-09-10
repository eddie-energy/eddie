package energy.eddie.regionconnector.us.green.button.client;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

class AtomFeedTransformer {
    private static final String XPATH_EXPR = "//content";
    private static final String TYPE_ATTRIBUTE = "type";
    private static final String TYPE_ATOM_XML = "atom+xml";

    public String transform(String payload) throws XPathExpressionException, ParserConfigurationException, IOException, TransformerException, SAXException {
        var factory = DocumentBuilderFactory.newInstance();
        var builder = factory.newDocumentBuilder();
        var doc = builder.parse(new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8)));
        var nodes = (NodeList) XPathFactory.newInstance().newXPath()
                                           .evaluate(XPATH_EXPR, doc, XPathConstants.NODESET);
        if (nodes.getLength() > 0) {
            for (int i = 0; i < nodes.getLength(); i++) {
                var node = nodes.item(i);
                if (node instanceof Element element && element.getAttribute(TYPE_ATTRIBUTE).isBlank()) {
                    element.setAttribute(TYPE_ATTRIBUTE, TYPE_ATOM_XML);
                }
            }
        }

        var tf = TransformerFactory.newInstance();
        var transformer = tf.newTransformer();
        var writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }
}
