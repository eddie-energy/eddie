package energy.eddie.regionconnector.fr.enedis.providers.v0;

import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveIntervalReading;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveMeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.agnostic.IdentifiableMeterReading;
import org.junit.jupiter.api.Test;
import reactor.adapter.JdkFlowAdapter;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnedisMvp1ConsumptionRecordProviderTest {

    @Test
    void testGetConsumptionRecordStream_returnsMappedRecords() throws Exception {
        // Given
        var intervalReading = new ConsumptionLoadCurveIntervalReading();
        intervalReading.setIntervalLength(ConsumptionLoadCurveIntervalReading.IntervalLengthEnum.PT5M);
        intervalReading.setValue("100");
        intervalReading.setMeasureType(ConsumptionLoadCurveIntervalReading.MeasureTypeEnum.B);
        var clcMeterReading = new ConsumptionLoadCurveMeterReading();
        clcMeterReading.setUsagePointId("uid");
        var start = ZonedDateTime.now(ZoneOffset.UTC);
        clcMeterReading.setStart(start.format(DateTimeFormatter.ISO_DATE));
        clcMeterReading.setIntervalReading(List.of(intervalReading));
        var meterReading = new IdentifiableMeterReading("pid", "cid", "dnid", clcMeterReading);
        TestPublisher<IdentifiableMeterReading> testPublisher = TestPublisher.create();

        // When
        var provider = new EnedisMvp1ConsumptionRecordProvider(testPublisher.flux());

        // Then
        StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(provider.getConsumptionRecordStream()))
                .then(() -> {
                    testPublisher.emit(meterReading);
                    testPublisher.complete();
                })
                .assertNext(consumptionRecord -> assertEquals("pid", consumptionRecord.getPermissionId()))
                .verifyComplete();

        // Clean-UP
        provider.close();
    }
}