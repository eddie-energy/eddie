package energy.eddie.regionconnector.be.fluvius.tasks;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.client.model.*;
import energy.eddie.regionconnector.be.fluvius.permission.events.MeterReadingUpdatedEvent;
import energy.eddie.regionconnector.be.fluvius.permission.request.MeterReading;
import energy.eddie.regionconnector.be.fluvius.persistence.MeterReadingRepository;
import energy.eddie.regionconnector.be.fluvius.streams.IdentifiableDataStreams;
import energy.eddie.regionconnector.be.fluvius.util.DefaultFluviusPermissionRequestBuilder;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeterReadingUpdateTaskTest {
    @Spy
    private final IdentifiableDataStreams streams = new IdentifiableDataStreams();
    @Mock
    private MeterReadingRepository meterReadingRepository;
    @Mock
    private Outbox outbox;
    @InjectMocks
    @SuppressWarnings("unused")
    private MeterReadingUpdateTask meterReadingUpdateTask;
    @Captor
    private ArgumentCaptor<List<MeterReading>> captor;

    @Test
    void testOnMeterReading_forUnknownMeter_doesNotUpdateMeterReading() {
        // Given
        var item = new EDailyEnergyItemResponseModel(null, null, null);
        var meter = new ElectricityMeterResponseModel(null, "002", List.of(item), List.of());
        var energy = new GetEnergyResponseModel(null, null, List.of(meter));
        var payload = new GetEnergyResponseModelApiDataResponse(null, energy);
        var permissionRequest = new DefaultFluviusPermissionRequestBuilder()
                .permissionId("pid")
                .addMeterReadings(new MeterReading("pid", "5001", null))
                .build();
        when(meterReadingRepository.findAllByPermissionId("pid")).thenReturn(permissionRequest.lastMeterReadings());

        // When
        streams.publish(permissionRequest, payload);

        // Then
        verify(meterReadingRepository).saveAllAndFlush(captor.capture());
        assertThat(captor.getValue())
                .singleElement()
                .extracting(MeterReading::lastMeterReading)
                .isNull();
    }

    @Test
    void testOnMeterReading_forMissingPayload_doesNotUpdateMeterReading() {
        // Given
        var payload = new GetEnergyResponseModelApiDataResponse();
        var permissionRequest = new DefaultFluviusPermissionRequestBuilder()
                .permissionId("pid")
                .addMeterReadings(new MeterReading("pid", "5001", null))
                .build();
        when(meterReadingRepository.findAllByPermissionId("pid")).thenReturn(permissionRequest.lastMeterReadings());

        // When
        streams.publish(permissionRequest, payload);

        // Then
        verify(meterReadingRepository).saveAllAndFlush(captor.capture());
    }

    @Test
    void testOnMeterReading_whereQuarterHourlyAndDailyValuesAreAvailableForTheSameMeter_updatesCorrectMeterReading() {
        // Given
        var end = ZonedDateTime.of(2025, 1, 31, 0, 0, 0, 0, ZoneOffset.UTC);
        var newEnd = ZonedDateTime.of(2025, 2, 20, 0, 0, 0, 0, ZoneOffset.UTC);
        var quarterHourlyItem = new EQuarterHourlyEnergyItemResponseModel(null, newEnd, null);
        var meter1 = new ElectricityMeterResponseModel(null, "001", null, List.of(quarterHourlyItem));
        var dailyItem = new EDailyEnergyItemResponseModel(null, null, null);
        var meter2 = new ElectricityMeterResponseModel(null, "001", List.of(dailyItem), null);
        var energy = new GetEnergyResponseModel(null, null, List.of(meter1, meter2));
        var payload = new GetEnergyResponseModelApiDataResponse(null, energy);
        var permissionRequest = new DefaultFluviusPermissionRequestBuilder()
                .permissionId("pid")
                .addMeterReadings(new MeterReading("pid", "5001", end))
                .build();
        when(meterReadingRepository.findAllByPermissionId("pid")).thenReturn(permissionRequest.lastMeterReadings());

        // When
        streams.publish(permissionRequest, payload);

        // Then
        verify(meterReadingRepository).saveAllAndFlush(captor.capture());
        assertThat(captor.getValue())
                .singleElement()
                .extracting(MeterReading::lastMeterReading)
                .isEqualTo(newEnd);
    }

    @Test
    void testOnMeterReading_readingWithoutReadings_updatesCorrectMeterReading() {
        // Given
        var end = OffsetDateTime.of(2025, 1, 31, 0, 0, 0, 0, ZoneOffset.UTC);
        var meter = new ElectricityMeterResponseModel(null, "001", null, null);
        var energy = new GetEnergyResponseModel(null, null, List.of(meter));
        var payload = new GetEnergyResponseModelApiDataResponse(null, energy);
        var permissionRequest = new DefaultFluviusPermissionRequestBuilder()
                .permissionId("pid")
                .addMeterReadings(new MeterReading("pid", "5001", end.toZonedDateTime()))
                .build();
        when(meterReadingRepository.findAllByPermissionId("pid")).thenReturn(permissionRequest.lastMeterReadings());

        // When
        streams.publish(permissionRequest, payload);

        // Then
        verify(meterReadingRepository).saveAllAndFlush(captor.capture());
        assertThat(captor.getValue())
                .singleElement()
                .extracting(MeterReading::lastMeterReading)
                .isEqualTo(end.toZonedDateTime());
    }

    @Test
    void testOnMeterReading_forGasMeters_updatesCorrectMeterReading() {
        // Given
        var end = ZonedDateTime.of(2025, 1, 31, 0, 0, 0, 0, ZoneOffset.UTC);
        var item1 = new GHourlyEnergyItemResponseModel(null, end.minusDays(1), null);
        var item2 = new GHourlyEnergyItemResponseModel(null, end, null);
        var item3 = new GHourlyEnergyItemResponseModel(null, end.minusDays(2), null);
        var meter = new GasMeterResponseModel(null, "001", null, List.of(item1, item2, item3));
        var energy = new GetEnergyResponseModel(null, List.of(meter), null);
        var payload = new GetEnergyResponseModelApiDataResponse(null, energy);
        var permissionRequest = new DefaultFluviusPermissionRequestBuilder()
                .granularity(Granularity.PT1H)
                .permissionId("pid")
                .addMeterReadings(new MeterReading("pid", "5001", end.minusDays(1)))
                .build();
        when(meterReadingRepository.findAllByPermissionId("pid")).thenReturn(permissionRequest.lastMeterReadings());

        // When
        streams.publish(permissionRequest, payload);

        // Then
        verify(meterReadingRepository).saveAllAndFlush(captor.capture());
        assertThat(captor.getValue())
                .singleElement()
                .extracting(MeterReading::lastMeterReading)
                .isEqualTo(end);
    }

    @Test
    void testOnMeterReading_emitsMeterReadingUpdatedEvent() {
        // Given
        var end = ZonedDateTime.of(2025, 1, 31, 0, 0, 0, 0, ZoneOffset.UTC);
        var item = new GHourlyEnergyItemResponseModel(null, end, null);
        var meter = new GasMeterResponseModel(null, "001", null, List.of(item));
        var energy = new GetEnergyResponseModel(null, List.of(meter), null);
        var payload = new GetEnergyResponseModelApiDataResponse(null, energy);
        var meterReading = new MeterReading("pid", "5001", end.minusDays(1));
        var permissionRequest = new DefaultFluviusPermissionRequestBuilder()
                .permissionId("pid")
                .status(PermissionProcessStatus.VALIDATED)
                .addMeterReadings(meterReading)
                .build();
        when(meterReadingRepository.findAllByPermissionId("pid")).thenReturn(permissionRequest.lastMeterReadings());

        // When
        streams.publish(permissionRequest, payload);

        // Then
        verify(outbox).commit(assertArg(res -> assertThat(res)
                .asInstanceOf(InstanceOfAssertFactories.type(MeterReadingUpdatedEvent.class))
                .satisfies(event -> {
                    assertThat(event.permissionId()).isEqualTo("pid");
                    assertThat(event.status()).isEqualTo(PermissionProcessStatus.VALIDATED);
                })));
    }
}