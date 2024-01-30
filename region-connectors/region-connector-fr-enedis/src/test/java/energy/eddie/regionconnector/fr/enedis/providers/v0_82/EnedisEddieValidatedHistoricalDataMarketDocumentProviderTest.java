package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.regionconnector.fr.enedis.config.PlainEnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveIntervalReading;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveMeterReading;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveReadingType;
import energy.eddie.regionconnector.fr.enedis.providers.agnostic.IdentifiableMeterReading;
import org.junit.jupiter.api.Test;
import reactor.adapter.JdkFlowAdapter;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnedisEddieValidatedHistoricalDataMarketDocumentProviderTest {
    @Test
    void testGetEddieValidatedHistoricalDataMarketDocumentStream_publishesDocuments() throws Exception {
        // Given
        var intervalReading = new ConsumptionLoadCurveIntervalReading();
        intervalReading.setIntervalLength(ConsumptionLoadCurveIntervalReading.IntervalLengthEnum.PT5M);
        intervalReading.setValue("100");
        intervalReading.setMeasureType(ConsumptionLoadCurveIntervalReading.MeasureTypeEnum.B);
        String date = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE);
        intervalReading.setDate(date);
        var clcMeterReading = new ConsumptionLoadCurveMeterReading();
        clcMeterReading.setUsagePointId("uid");
        var start = ZonedDateTime.now(ZoneOffset.UTC).minusDays(10).format(IntermediateValidatedHistoricalDocument.ENEDIS_DATE_FORMAT);
        var end = ZonedDateTime.now(ZoneOffset.UTC).minusDays(1).format(IntermediateValidatedHistoricalDocument.ENEDIS_DATE_FORMAT);
        clcMeterReading.setStart(start);
        clcMeterReading.setEnd(end);
        ConsumptionLoadCurveReadingType readingType = new ConsumptionLoadCurveReadingType();
        readingType.setMeasurementKind("power");
        readingType.setAggregate("sum");
        clcMeterReading.setReadingType(readingType);
        var meterReading = new IdentifiableMeterReading("pid", "cid", "dnid", clcMeterReading);
        clcMeterReading.setIntervalReading(List.of(intervalReading));
        TestPublisher<IdentifiableMeterReading> testPublisher = TestPublisher.create();
        PlainEnedisConfiguration enedisConfiguration = new PlainEnedisConfiguration(
                "clientId",
                "clientSecret",
                "/path"
        );
        IntermediateVHDFactory factory = new IntermediateVHDFactory(
                enedisConfiguration,
                () -> CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME
        );
        var provider = new EnedisEddieValidatedHistoricalDataMarketDocumentProvider(testPublisher.flux(), factory);

        // When
        StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(provider.getEddieValidatedHistoricalDataMarketDocumentStream()))
                .then(() -> {
                    testPublisher.emit(meterReading);
                    testPublisher.complete();
                })
                .assertNext(vhd -> {
                    assertTrue(vhd.permissionId().isPresent());
                    assertEquals("pid", vhd.permissionId().get());
                })
                .verifyComplete();

        // Clean-Up
        provider.close();
    }
}