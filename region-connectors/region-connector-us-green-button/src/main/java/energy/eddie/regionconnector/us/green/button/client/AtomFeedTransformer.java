// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.client;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
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

    /**
     * Atom feeds contain entries, where each entry can contain 0...n contents. The content element has a type attribute
     * that indicates the content type of the content element. The green button API is missing the type attribute in
     * some cases. Therefore, we have to manually set the type attribute in order for rome to be able to parse the feed
     * correctly. The type is set to {@code atom+xml}.
     *
     * @param payload XML String
     * @return XMl String, where each content has a type attribute
     * @throws XPathExpressionException     if the XPATH is not valid
     * @throws ParserConfigurationException if the parser is not correctly configured
     * @throws IOException                  if the {@code payload} cannot be read by the parser
     * @throws TransformerException         if the resulting XML cannot be stringified
     * @throws SAXException                 if the {@code payload} cannot be parsed
     */
    public String transform(String payload) throws XPathExpressionException, ParserConfigurationException, IOException, TransformerException, SAXException {
        // Secure factories against XXE
        var factory = DocumentBuilderFactory.newInstance();
        factory.setExpandEntityReferences(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
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
        var transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        transformerFactory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        var transformer = transformerFactory.newTransformer();
        var writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }
}
