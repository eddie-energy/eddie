// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.client;

import energy.eddie.regionconnector.us.green.button.XmlLoader;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtomFeedTransformerTest {

    @Test
    void transformWithoutTypeOnContent_addsType() throws XPathExpressionException, ParserConfigurationException, IOException, TransformerException, SAXException {
        // Given
        var transformer = new AtomFeedTransformer();
        var xml = XmlLoader.xmlFromResource("/xml/usagepoint/UsagePointWithoutType.xml");

        // When
        var res = transformer.transform(xml);

        // Then
        assertTrue(res.contains("type=\"atom+xml\""));
    }

    @Test
    void transformWithJsonContentType_doesNotChangeContent() throws XPathExpressionException, ParserConfigurationException, IOException, TransformerException, SAXException {
        // Given
        var transformer = new AtomFeedTransformer();
        var xml = XmlLoader.xmlFromResource("/xml/usagepoint/UsagePointWithJsonContentType.xml");

        // When
        var res = transformer.transform(xml);

        // Then
        assertFalse(res.contains("type=\"atom+xml\""));
    }
}