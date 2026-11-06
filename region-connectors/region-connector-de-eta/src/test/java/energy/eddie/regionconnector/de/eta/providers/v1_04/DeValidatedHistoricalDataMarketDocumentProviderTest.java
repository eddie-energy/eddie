package energy.eddie.regionconnector.de.eta.providers.v1_04;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.serde.XmlMessageSerde;
import energy.eddie.cim.testing.XmlValidator;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.cim.v1_04.vhd.VHDEnvelope;
import energy.eddie.cim.v1_12.vhd.Envelope;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import energy.eddie.regionconnector.de.eta.providers.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeValidatedHistoricalDataMarketDocumentProviderTest {
    @Mock
    private ValidatedHistoricalDataStream stream;

    private static final ZonedDateTime T0 = ZonedDateTime.of(2026, 4, 30, 22, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void getValidatedHistoricalDataMarketDocumentsStream_emitsOneEnvelopePerInputPayload() {
        var stream = mock(ValidatedHistoricalDataStream.class);
        when(stream.validatedHistoricalData()).thenReturn(Flux.just(samplePayload()));

        var provider = new DeValidatedHistoricalDataMarketDocumentProvider(
                stream,
                new PlainCommonInformationModelConfiguration(
                        CodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME, "fallback"),
                new DeEtaPlusConfiguration(
                        "ep-id", "http://api.url", "client-id", "client-secret",
                        "/meters/historical", "/meters/accounting-point", 30,
                        3, 2, true, false, null, null)
        );

        StepVerifier.create(provider.getValidatedHistoricalDataMarketDocumentsStream())
                .expectNextCount(1)
                .verifyComplete();
    }

    private static IdentifiableValidatedHistoricalData samplePayload() {
        DePermissionRequest pr = new DePermissionRequestBuilder()
                .permissionId("pid").connectionId("cid").meteringPointId("mp")
                .start(LocalDate.of(2026, 4, 30)).end(LocalDate.of(2026, 5, 1))
                .granularity(Granularity.PT15M).energyType(EnergyType.ELECTRICITY)
                .status(PermissionProcessStatus.ACCEPTED).created(T0).dataNeedId("dnid")
                .build();
        var reading = new EtaPlusMeteredData.MeterReading(T0, 1.0, "kWh", "VALIDATED", "Consumption");
        var payload = new EtaPlusMeteredData("mp", pr.start(), pr.end(), List.of(reading));
        return new IdentifiableValidatedHistoricalData(pr, payload);
    }

    @Test
    void getValidatedHistoricalDataMarketDocumentsStream_emitsOneEnvelopePerInputPayload_alsoEmitsEquivalentV112() {
        var publisher = TestPublisher.<IdentifiableValidatedHistoricalData>create();
        when(stream.validatedHistoricalData()).thenReturn(publisher.flux());

        var provider = new DeValidatedHistoricalDataMarketDocumentProvider(
                stream,
                new PlainCommonInformationModelConfiguration(
                        CodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME, "fallback"),
                new DeEtaPlusConfiguration(
                        "ep-id", "http://api.url", "client-id", "client-secret",
                        "/meters/historical", "/meters/accounting-point", 30,
                        3, 2, true, false, null, null)
        );

        var pairedDocuments = Flux.zip(provider.getValidatedHistoricalDataMarketDocumentsStream(),
                                       provider.getValidatedHistoricalDataV112Stream());
        StepVerifier.create(pairedDocuments)
                .then(() -> publisher.emit(samplePayload()))
                .assertNext(pair -> assertEquivalentHistoricalData(pair.getT1(), pair.getT2()))
                .verifyComplete();
    }

    private static void assertEquivalentHistoricalData(VHDEnvelope source, Envelope target) {
        assertEquals(source.getMarketDocument().getMRID(), target.getVHDMarketDocument().getMRID());
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
            assertEquals(
                    sourceTimeSeries.getEnergyMeasurementUnitName(),
                    targetTimeSeries.getEnergyMeasurementUnitName()
            );
            assertEquals(
                    sourceTimeSeries.getEnergyQualityMeasurementUnitName(),
                    targetTimeSeries.getEnergyQualityMeasurementUnitName()
            );
            assertEquals(sourceTimeSeries.getFlowDirectionDirection(), targetTimeSeries.getFlowDirectionDirection());
            assertEquals(1, sourceTimeSeries.getPeriods().size(), "Fixture periods per time series");
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
        });
    }
}
