// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.atom.feed;

import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import energy.eddie.regionconnector.us.green.button.XmlLoader;
import org.junit.jupiter.api.Test;
import org.naesb.espi.UsagePoint;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.xml.sax.InputSource;

import static org.junit.jupiter.api.Assertions.*;

class QueryTest {

    @Test
    void testFindAllByTitle_returnsAllByTitle() throws FeedException {
        // Given
        var xml = XmlLoader.xmlStreamFromResource("/xml/batch/Batch.xml");
        var feed = new SyndFeedInput().build(new InputSource(xml));
        var query = new Query(feed, new Jaxb2Marshaller().createUnmarshaller());

        // When
        var res = query.findAllByTitle("UsagePoint");

        // Then
        assertEquals(1, res.size());
    }

    @Test
    void testUnmarshall_returnsObject() throws FeedException {
        // Given
        var xml = XmlLoader.xmlStreamFromResource("/xml/batch/Batch.xml");
        var feed = new SyndFeedInput().build(new InputSource(xml));
        var marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("org.naesb.espi");
        var query = new Query(feed, marshaller.createUnmarshaller());
        var usagePoint = query.findAllByTitle("UsagePoint").getFirst();

        // When
        var res = query.unmarshal(usagePoint, UsagePoint.class);

        // Then
        //noinspection DataFlowIssue
        assertAll(
                () -> assertNotNull(res),
                () -> assertEquals("0", res.getServiceCategory().getKind())
        );
    }

    @Test
    void testUnmarshall_returnsNull_onInvalidObject() throws FeedException {
        // Given
        var xml = XmlLoader.xmlStreamFromResource("/xml/batch/Batch.xml");
        var feed = new SyndFeedInput().build(new InputSource(xml));
        var marshaller = new Jaxb2Marshaller();
        var query = new Query(feed, marshaller.createUnmarshaller());
        var usagePoint = query.findAllByTitle("UsagePoint").getFirst();

        // When
        var res = query.unmarshal(usagePoint, UsagePoint.class);

        // Then
        assertNull(res);
    }

    @Test
    void testUnmarshall_returnsNull_onEmptyEntry() throws FeedException {
        // Given
        var xml = XmlLoader.xmlStreamFromResource("/xml/batch/Batch.xml");
        var feed = new SyndFeedInput().build(new InputSource(xml));
        var marshaller = new Jaxb2Marshaller();
        var query = new Query(feed, marshaller.createUnmarshaller());
        var entry = new SyndEntryImpl();

        // When
        var res = query.unmarshal(entry, UsagePoint.class);

        // Then
        assertNull(res);
    }

    @Test
    void testFindFirstBySelfLinkAndTitle_returnsFirstObject() throws FeedException {
        // Given
        var xml = XmlLoader.xmlStreamFromResource("/xml/batch/Batch.xml");
        var feed = new SyndFeedInput().build(new InputSource(xml));
        var marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("org.naesb.espi");
        var query = new Query(feed, marshaller.createUnmarshaller());

        // When
        var res = query.findFirstBySelfLinkAndTitle(
                "https://utilityapi.com/DataCustodian/espi/1_1/resource/Subscription/467189",
                "UsagePoint",
                UsagePoint.class);

        // Then
        assertTrue(res.isPresent());
        var usagePoint = res.get();
        assertEquals("0", usagePoint.getServiceCategory().getKind());
    }

    @Test
    void testFindFirstBySelfLinkAndTitle_returnsEmptyOptional_withInvalidSelfLink() throws FeedException {
        // Given
        var xml = XmlLoader.xmlStreamFromResource("/xml/batch/Batch.xml");
        var feed = new SyndFeedInput().build(new InputSource(xml));
        var marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("org.naesb.espi");
        var query = new Query(feed, marshaller.createUnmarshaller());

        // When
        var res = query.findFirstBySelfLinkAndTitle(
                "https://utilityapi.com/DataCustodian/espi/1_1/resource/Subscription/blblblbl",
                "UsagePoint",
                UsagePoint.class);

        // Then
        assertTrue(res.isEmpty());
    }

    @Test
    void testFindFirstBySelfLinkAndTitle_returnsEmptyOptional_withInvalidName() throws FeedException {
        // Given
        var xml = XmlLoader.xmlStreamFromResource("/xml/batch/Batch.xml");
        var feed = new SyndFeedInput().build(new InputSource(xml));
        var marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("org.naesb.espi");
        var query = new Query(feed, marshaller.createUnmarshaller());

        // When
        var res = query.findFirstBySelfLinkAndTitle(
                "https://utilityapi.com/DataCustodian/espi/1_1/resource/Subscription/467189",
                "InvalidObject",
                UsagePoint.class);

        // Then
        assertTrue(res.isEmpty());
    }
}