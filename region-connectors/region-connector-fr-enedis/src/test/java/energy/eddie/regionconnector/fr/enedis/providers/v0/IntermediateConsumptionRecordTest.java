package energy.eddie.regionconnector.fr.enedis.providers.v0;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.dto.IntervalReading;
import energy.eddie.regionconnector.fr.enedis.dto.MeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.MeterReadingType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static energy.eddie.api.v0.ConsumptionPoint.MeteringType.MEASURED_VALUE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IntermediateConsumptionRecordTest {

    @ParameterizedTest
    @EnumSource(Granularity.class)
    void testConsumptionRecord_withIntervalsAndMeasurementTypes(Granularity intervalLength) {
        // Given
        var intervalReading = new IntervalReading("100",
                                                  "2024-02-26 00:30:00",
                                                  Optional.of("B"),
                                                  Optional.of(intervalLength.name()));
        var clcMeterReading = new MeterReading("uid",
                                               LocalDate.now(ZoneOffset.UTC),
                                               LocalDate.now(ZoneOffset.UTC),
                                               "BRUT",
                                               null,
                                               List.of(intervalReading));
        var permissionRequest = mock(FrEnedisPermissionRequest.class);
        when(permissionRequest.connectionId()).thenReturn("cid");
        when(permissionRequest.permissionId()).thenReturn("pid");
        when(permissionRequest.dataNeedId()).thenReturn("dnid");
        when(permissionRequest.granularity()).thenReturn(intervalLength);

        var meterReading = new IdentifiableMeterReading(permissionRequest,
                                                        clcMeterReading,
                                                        MeterReadingType.CONSUMPTION);
        var intermediateRecord = new IntermediateConsumptionRecord(meterReading);

        // When
        var result = intermediateRecord.consumptionRecord();

        // Then
        assertAll(
                () -> assertEquals("pid", result.getPermissionId()),
                () -> assertEquals("cid", result.getConnectionId()),
                () -> assertEquals("dnid", result.getDataNeedId()),
                () -> assertEquals(1, result.getConsumptionPoints().size()),
                () -> assertEquals(100, result.getConsumptionPoints().getFirst().getConsumption()),
                () -> assertEquals(MEASURED_VALUE, result.getConsumptionPoints().getFirst().getMeteringType()),
                () -> assertEquals(intervalLength.name(), result.getMeteringInterval())
        );
    }

    @Test
    void testConsumptionRecord_withEmptyIntervalReading() {
        // Given
        var clcMeterReading = new MeterReading("uid",
                                               LocalDate.now(ZoneOffset.UTC),
                                               LocalDate.now(ZoneOffset.UTC),
                                               "BRUT",
                                               null,
                                               List.of());
        var permissionRequest = mock(FrEnedisPermissionRequest.class);
        when(permissionRequest.connectionId()).thenReturn("cid");
        when(permissionRequest.permissionId()).thenReturn("pid");
        when(permissionRequest.dataNeedId()).thenReturn("dnid");

        var meterReading = new IdentifiableMeterReading(permissionRequest,
                                                        clcMeterReading,
                                                        MeterReadingType.CONSUMPTION);
        var intermediateRecord = new IntermediateConsumptionRecord(meterReading);

        // When, Then
        assertThrows(IllegalStateException.class, intermediateRecord::consumptionRecord);
    }
}
