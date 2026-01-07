package energy.eddie.regionconnector.us.green.button.providers.v1_04;

import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.serde.SerdeInitializationException;
import energy.eddie.cim.serde.SerializationException;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.us.green.button.GreenButtonPermissionRequestBuilder;
import energy.eddie.regionconnector.us.green.button.XmlLoader;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.providers.IdentifiableSyndFeed;
import energy.eddie.regionconnector.us.green.button.providers.UnsupportedUnitException;
import org.junit.jupiter.api.Test;

import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.xml.sax.InputSource;
import org.xmlunit.builder.DiffBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IntermediateValidatedHistoricalDataMarketDocumentTest {
    private final XmlMessageSerde serde = new XmlMessageSerde();
    private final CommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
            CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, "id"
    );
    private final GreenButtonConfiguration config = new GreenButtonConfiguration(
            "http://localhost",
            Map.of("company", "client-id"),
            Map.of("company", "client-secret"),
            Map.of("company", "token"),
            "http://localhost",
            "secret"
    );

    IntermediateValidatedHistoricalDataMarketDocumentTest() throws SerdeInitializationException { }

    @Test
    @SuppressWarnings("java:S5961")
    void testToVhd_returnsValidatedHistoricalDataMarketDocument() throws SerializationException, UnsupportedUnitException, FeedException, IOException {
        // Given
        var xml = XmlLoader.xmlStreamFromResource("/xml/batch/Batch.xml");
        var feed = new SyndFeedInput().build(new InputSource(xml));
        var marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("org.naesb.espi");
        var pr = new GreenButtonPermissionRequestBuilder().setPermissionId("pid")
                .setConnectionId("cid")
                .setDataNeedId("dnid")
                .setCountryCode("US")
                .setCompanyId("company")
                .build();
        var intermediateVhd = new IntermediateValidatedHistoricalDataMarketDocument(
                new IdentifiableSyndFeed(pr, feed),
                marshaller,
                cimConfig,
                config
        );

        // When
        var res = intermediateVhd.toVhd();

        // Then
        var bytes = serde.serialize(res.getFirst());
        var testXml = new String(bytes, StandardCharsets.UTF_8);

        String expected;
        try (InputStream is = XmlLoader.xmlStreamFromResource("/xml/cim/output_vhd_v1.04.xml")) {
            expected = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        var myDiff = DiffBuilder.compare(expected)
                .withTest(testXml)
                .ignoreWhitespace()
                .ignoreComments()
                .checkForSimilar()
                .withNodeFilter(node -> ignoredNames().stream().noneMatch(node.getNodeName()::endsWith))
                .build();
        assertFalse(myDiff.hasDifferences(), myDiff.fullDescription());
        assertTrue(XmlValidator.validateV104ValidatedHistoricalDataMarketDocument(bytes));
    }

    private static Set<String> ignoredNames() {
        return Set.of(
                "messageDocumentHeader.creationDateTime",
                "createdDateTime",
                "mRID"
        );
    }
}