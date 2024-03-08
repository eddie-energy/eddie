package energy.eddie.regionconnector.fr.enedis.providers.v0;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.fr.enedis.dto.IntervalReading;
import energy.eddie.regionconnector.fr.enedis.dto.MeterReading;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnedisMvp1ConsumptionRecordProviderTest {

    @Test
    void testGetConsumptionRecordStream_returnsMappedRecords() throws Exception {
        // Given
        var intervalReading = new IntervalReading("100", "2024-02-26 00:30:00", Optional.of("B"), Optional.of(Granularity.PT30M.name()));
        var clcMeterReading = new MeterReading("uid", LocalDate.now(ZoneOffset.UTC), LocalDate.now(ZoneOffset.UTC), "BRUT", null, List.of(intervalReading));
        var permissionRequest = mock(FrEnedisPermissionRequest.class);
        when(permissionRequest.connectionId()).thenReturn("cid");
        when(permissionRequest.permissionId()).thenReturn("pid");
        when(permissionRequest.dataNeedId()).thenReturn("dnid");
        when(permissionRequest.granularity()).thenReturn(Granularity.PT30M);

        var meterReading = new IdentifiableMeterReading(permissionRequest, clcMeterReading);
        TestPublisher<IdentifiableMeterReading> testPublisher = TestPublisher.create();

        // When
        var provider = new EnedisMvp1ConsumptionRecordProvider(testPublisher.flux());

        // Then
        StepVerifier.create(provider.getConsumptionRecordStream())
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