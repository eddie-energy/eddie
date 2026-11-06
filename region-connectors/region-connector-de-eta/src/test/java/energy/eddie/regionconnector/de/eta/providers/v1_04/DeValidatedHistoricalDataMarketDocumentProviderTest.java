package energy.eddie.regionconnector.de.eta.providers.v1_04;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import energy.eddie.regionconnector.de.eta.providers.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeValidatedHistoricalDataMarketDocumentProviderTest {

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

        StepVerifier.create(reactor.core.publisher.Flux.zip(provider.getValidatedHistoricalDataMarketDocumentsStream(), provider.getValidatedHistoricalDataV112Stream()).map(pair -> {
            var source = pair.getT1();
            var target = pair.getT2();
            org.junit.jupiter.api.Assertions.assertEquals(source.getMessageDocumentHeaderMetaInformationPermissionId(), target.getMessageDocumentHeader().getMetaInformation().getRequestPermissionId());
            org.junit.jupiter.api.Assertions.assertEquals(source.getMessageDocumentHeaderMetaInformationDataNeedId(), target.getMessageDocumentHeader().getMetaInformation().getDataNeedId());
            var sourceSeries = source.getMarketDocument().getTimeSeries();
            var targetSeries = target.getVHDMarketDocument().getTimeSeries();
            org.junit.jupiter.api.Assertions.assertEquals(sourceSeries.size(), targetSeries.size());
            for (int i = 0; i < sourceSeries.size(); i++) {
                var left = sourceSeries.get(i);
                var right = targetSeries.get(i);
                org.junit.jupiter.api.Assertions.assertEquals(left.getEnergyMeasurementUnitName(), right.getEnergyMeasurementUnitName());
                org.junit.jupiter.api.Assertions.assertEquals(left.getEnergyQualityMeasurementUnitName(), right.getEnergyQualityMeasurementUnitName());
                org.junit.jupiter.api.Assertions.assertEquals(left.getFlowDirectionDirection(), right.getFlowDirectionDirection());
                org.junit.jupiter.api.Assertions.assertEquals(left.getPeriods().size(), right.getSeriesPeriods().size());
                for (int j = 0; j < left.getPeriods().size(); j++) {
                    var lp = left.getPeriods().get(j);
                    var rp = right.getSeriesPeriods().get(j);
                    org.junit.jupiter.api.Assertions.assertEquals(lp.getResolution(), rp.getResolution());
                    org.junit.jupiter.api.Assertions.assertEquals(lp.getTimeInterval().getStart(), rp.getTimeInterval().getStart());
                    org.junit.jupiter.api.Assertions.assertEquals(lp.getTimeInterval().getEnd(), rp.getTimeInterval().getEnd());
                    org.junit.jupiter.api.Assertions.assertEquals(lp.getPoints().size(), rp.getPoints().size());
                    for (int k = 0; k < lp.getPoints().size(); k++) {
                        var l = lp.getPoints().get(k);
                        var r = rp.getPoints().get(k);
                        org.junit.jupiter.api.Assertions.assertEquals(l.getPosition(), r.getPosition());
                        org.junit.jupiter.api.Assertions.assertEquals(l.getEnergyQuantityQuantity(), r.getEnergyQuantityQuantity());
                        org.junit.jupiter.api.Assertions.assertEquals(l.getEnergyQuantityQuality(), r.getEnergyQuantityQuality());
                        org.junit.jupiter.api.Assertions.assertEquals(l.getEnergyQualityQuantityQuantity(), r.getEnergyQualityQuantityQuantity());
                        org.junit.jupiter.api.Assertions.assertEquals(l.getEnergyQualityQuantityQuality(), r.getEnergyQualityQuantityQuality());
                    }
                }
            }
            if (!targetSeries.isEmpty()) {
                org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
                    var serialized = new energy.eddie.cim.serde.XmlMessageSerde().serialize(target);
                    org.junit.jupiter.api.Assertions.assertTrue(energy.eddie.cim.testing.XmlValidator.validateV112ValidatedHistoricalDataMarketDocument(serialized));
                });
            }
            return target;
        }))
                .expectNextCount(1)
                .verifyComplete();
    }
}