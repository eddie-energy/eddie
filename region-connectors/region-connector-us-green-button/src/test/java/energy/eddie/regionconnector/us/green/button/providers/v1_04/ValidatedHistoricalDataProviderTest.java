// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.providers.v1_04;

import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.serde.JsonMessageSerde;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.us.green.button.GreenButtonPermissionRequestBuilder;
import energy.eddie.regionconnector.us.green.button.XmlLoader;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.providers.IdentifiableSyndFeed;
import energy.eddie.regionconnector.us.green.button.services.PublishService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.xml.sax.InputSource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ValidatedHistoricalDataProviderTest {
    @Spy
    private final PublishService publishService = new PublishService();
    @Spy
    private final Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
    @SuppressWarnings("unused")
    @Spy
    private final CommonInformationModelConfiguration cimConfig = new PlainCommonInformationModelConfiguration(
            CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME, "client-id"
    );
    @SuppressWarnings("unused")
    @Spy
    private final GreenButtonConfiguration config = new GreenButtonConfiguration(
            "http://localhost",
            Map.of("company", "client-id"),
            Map.of("company", "client-secret"),
            Map.of("company", "token"),
            "http://localhost",
            "secret");
    @InjectMocks
    private ValidatedHistoricalDataProvider provider;

    @Test
    void testReceivesIdentifiablePayload_emitsValidatedHistoricalDataMarketDocument() throws FeedException {
        // Given
        marshaller.setPackagesToScan("org.naesb.espi");
        var xml = XmlLoader.xmlStreamFromResource("/xml/batch/Batch.xml");
        var feed = new SyndFeedInput().build(new InputSource(xml));
        var permissionRequest = new GreenButtonPermissionRequestBuilder()
                .setPermissionId("pid")
                .setConnectionId("cid")
                .setDataNeedId("dnid")
                .setCountryCode("US")
                .setCompanyId("company")
                .build();

        // When
        publishService.publishValidatedHistoricalData(new IdentifiableSyndFeed(permissionRequest, feed));

        // Then
        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream())
                    .then(publishService::close)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testReceivesIdentifiablePayload_emitsValidatedHistoricalDataMarketDocument_alsoEmitsEquivalentV112(boolean subscribeV112First) throws FeedException {
        // Given
        marshaller.setPackagesToScan("org.naesb.espi");
        var xml = XmlLoader.xmlStreamFromResource("/xml/batch/Batch.xml");
        var feed = new SyndFeedInput().build(new InputSource(xml));
        var permissionRequest = new GreenButtonPermissionRequestBuilder()
                .setPermissionId("pid")
                .setConnectionId("cid")
                .setDataNeedId("dnid")
                .setCountryCode("US")
                .setCompanyId("company")
                .build();

        // When

        // Then
        var providerV112 = new energy.eddie.regionconnector.us.green.button.providers.v1_12.ValidatedHistoricalDataProvider(publishService, marshaller, cimConfig, config);
        var pairedDocuments = subscribeV112First
                ? Flux.zip(providerV112.getValidatedHistoricalDataV112Stream(), provider.getValidatedHistoricalDataMarketDocumentsStream())
                      .map(pair -> reactor.util.function.Tuples.of(pair.getT2(), pair.getT1()))
                : Flux.zip(provider.getValidatedHistoricalDataMarketDocumentsStream(), providerV112.getValidatedHistoricalDataV112Stream());
        StepVerifier.create(pairedDocuments)
                    .then(() -> publishService.publishValidatedHistoricalData(new IdentifiableSyndFeed(permissionRequest, feed)))
                    .then(publishService::close)
                    .assertNext(pair -> assertEquivalentHistoricalData(pair.getT1(), pair.getT2()))
                    .verifyComplete();
    }

    private static void assertEquivalentHistoricalData(VHDEnvelope source, Envelope target) {
        assertEquals(source.getMarketDocument().getMRID(), target.getVHDMarketDocument().getMRID());
        assertEquals(source.getMarketDocument().getCreatedDateTime(), target.getVHDMarketDocument().getCreatedDateTime());
        assertEquals(source.getMarketDocument().getRevisionNumber(), target.getVHDMarketDocument().getRevisionNumber());
        assertEquals(source.getMessageDocumentHeaderCreationDateTime(), target.getMessageDocumentHeader().getCreationDateTime());
        assertEquals(
                source.getMessageDocumentHeaderMetaInformationPermissionId(),
                target.getMessageDocumentHeader().getMetaInformation().getRequestPermissionId()
        );
        assertEquals(
                source.getMessageDocumentHeaderMetaInformationDataNeedId(),
                target.getMessageDocumentHeader().getMetaInformation().getDataNeedId()
        );
        var sourceSeries = source.getMarketDocument().getTimeSeries();
        var targetSeries = target.getVHDMarketDocument().getTimeSeries();
        assertEquals(1, sourceSeries.size(), "Fixture time series");
        assertEquals(sourceSeries.size(), targetSeries.size());
        for (int i = 0; i < sourceSeries.size(); i++) {
            var sourceTimeSeries = sourceSeries.get(i);
            var targetTimeSeries = targetSeries.get(i);
            assertEquals(sourceTimeSeries.getMRID(), targetTimeSeries.getMRID());
            assertEquals(sourceTimeSeries.getBusinessType(), targetTimeSeries.getBusinessType());
            assertEquals(sourceTimeSeries.getProduct(), targetTimeSeries.getProduct());
            assertEquals(sourceTimeSeries.getMarketEvaluationPointMRID().getValue(), targetTimeSeries.getAccountingPointMRID().getValue());
            assertEquals(sourceTimeSeries.getMarketEvaluationPointMRID().getCodingScheme(), targetTimeSeries.getAccountingPointMRID().getCodingScheme());
            assertEquals(source.getMarketDocument().getCreatedDateTime(), targetTimeSeries.getDateAndOrTimeDateTime());
            assertEquals(
                    sourceTimeSeries.getEnergyMeasurementUnitName(),
                    targetTimeSeries.getEnergyMeasurementUnitName()
            );
            assertEquals(
                    sourceTimeSeries.getEnergyQualityMeasurementUnitName(),
                    targetTimeSeries.getEnergyQualityMeasurementUnitName()
            );
            assertEquals(sourceTimeSeries.getFlowDirectionDirection(), targetTimeSeries.getFlowDirectionDirection());
            assertEquals(67, sourceTimeSeries.getPeriods().size(), "Fixture periods per time series");
            assertEquals(sourceTimeSeries.getPeriods().size(), targetTimeSeries.getSeriesPeriods().size());
            for (int j = 0; j < sourceTimeSeries.getPeriods().size(); j++) {
                var sourcePeriod = sourceTimeSeries.getPeriods().get(j);
                var targetPeriod = targetTimeSeries.getSeriesPeriods().get(j);
                assertEquals(sourcePeriod.getResolution(), targetPeriod.getResolution());
                assertEquals(sourcePeriod.getTimeInterval().getStart(), targetPeriod.getTimeInterval().getStart());
                assertEquals(sourcePeriod.getTimeInterval().getEnd(), targetPeriod.getTimeInterval().getEnd());
                assertEquals(1, sourcePeriod.getPoints().size(), "Fixture points per period");
                assertEquals(sourcePeriod.getPoints().size(), targetPeriod.getPoints().size());
                for (int k = 0; k < sourcePeriod.getPoints().size(); k++) {
                    var sourcePoint = sourcePeriod.getPoints().get(k);
                    var targetPoint = targetPeriod.getPoints().get(k);
                    assertEquals(sourcePoint.getPosition(), targetPoint.getPosition());
                    assertEquals(sourcePoint.getEnergyQuantityQuantity(), targetPoint.getEnergyQuantityQuantity());
                    assertEquals(sourcePoint.getEnergyQuantityQuality(), targetPoint.getEnergyQuantityQuality());
                    assertEquals(
                            sourcePoint.getEnergyQualityQuantityQuantity(),
                            targetPoint.getEnergyQualityQuantityQuantity()
                    );
                    assertEquals(
                            sourcePoint.getEnergyQualityQuantityQuality(),
                            targetPoint.getEnergyQualityQuantityQuality()
                    );
                }
            }
        }
        assertDoesNotThrow(() -> {
            var serialized = new XmlMessageSerde().serialize(target);
            assertTrue(XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(serialized));
            var xmlRoundTrip = new XmlMessageSerde().deserialize(serialized, Envelope.class);
            var json = new JsonMessageSerde();
            var jsonRoundTrip = json.deserialize(json.serialize(target), Envelope.class);
            assertEquals(target.getVHDMarketDocument().getMRID(), xmlRoundTrip.getVHDMarketDocument().getMRID());
            assertEquals(target.getVHDMarketDocument().getMRID(), jsonRoundTrip.getVHDMarketDocument().getMRID());
            assertEquals(target.getVHDMarketDocument().getTimeSeries().size(), xmlRoundTrip.getVHDMarketDocument().getTimeSeries().size());
            assertEquals(target.getVHDMarketDocument().getTimeSeries().size(), jsonRoundTrip.getVHDMarketDocument().getTimeSeries().size());
        });
    }
}
