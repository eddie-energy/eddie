package energy.eddie.regionconnector.be.fluvius.tasks;

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
        var item = new EDailyEnergyItemResponseModel();
        var meter = new ElectricityMeterResponseModel().meterID("002").addDailyEnergyItem(item);
        var energy = new GetEnergyResponseModel().addElectricityMetersItem(meter);
        var payload = new GetEnergyResponseModelApiDataResponse().data(energy);
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
        var end = OffsetDateTime.of(2025, 1, 31, 0, 0, 0, 0, ZoneOffset.UTC);
        var newEnd = OffsetDateTime.of(2025, 2, 20, 0, 0, 0, 0, ZoneOffset.UTC);
        var item = new EQuarterHourlyEnergyItemResponseModel()
                .timestampEnd(newEnd);
        var meter1 = new ElectricityMeterResponseModel()
                .meterID("001")
                .addQuarterHourlyEnergyItem(item);
        var meter2 = new ElectricityMeterResponseModel()
                .meterID("001")
                .addDailyEnergyItem(new EDailyEnergyItemResponseModel());
        var energy = new GetEnergyResponseModel()
                .addElectricityMetersItem(meter1)
                .addElectricityMetersItem(meter2);
        var payload = new GetEnergyResponseModelApiDataResponse()
                .data(energy);
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
                .isEqualTo(newEnd.toZonedDateTime());
    }

    @Test
    void testOnMeterReading_readingWithoutEndDate_updatesCorrectMeterReading() {
        // Given
        var end = OffsetDateTime.of(2025, 1, 31, 0, 0, 0, 0, ZoneOffset.UTC);
        var meter = new ElectricityMeterResponseModel()
                .meterID("001")
                .addQuarterHourlyEnergyItem(
                        new EQuarterHourlyEnergyItemResponseModel()
                );
        var energy = new GetEnergyResponseModel()
                .addElectricityMetersItem(meter);
        var payload = new GetEnergyResponseModelApiDataResponse()
                .data(energy);
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
    void testOnMeterReading_readingWithoutReadings_updatesCorrectMeterReading() {
        // Given
        var end = OffsetDateTime.of(2025, 1, 31, 0, 0, 0, 0, ZoneOffset.UTC);
        var meter = new ElectricityMeterResponseModel()
                .meterID("001");
        var energy = new GetEnergyResponseModel()
                .addElectricityMetersItem(meter);
        var payload = new GetEnergyResponseModelApiDataResponse()
                .data(energy);
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
        var end = OffsetDateTime.of(2025, 1, 31, 0, 0, 0, 0, ZoneOffset.UTC);
        var item = new GHourlyEnergyItemResponseModel()
                .timestampEnd(end);
        var meter = new GasMeterResponseModel()
                .meterID("001")
                .addHourlyEnergyItem(item);
        var energy = new GetEnergyResponseModel()
                .addGasMetersItem(meter);
        var payload = new GetEnergyResponseModelApiDataResponse()
                .data(energy);
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
                .isEqualTo(end.toZonedDateTime());
    }

    @Test
    void testOnMeterReading_emitsMeterReadingUpdatedEvent() {
        // Given
        var end = OffsetDateTime.of(2025, 1, 31, 0, 0, 0, 0, ZoneOffset.UTC);
        var item = new GHourlyEnergyItemResponseModel()
                .timestampEnd(end);
        var meter = new GasMeterResponseModel()
                .meterID("001")
                .addHourlyEnergyItem(item);
        var energy = new GetEnergyResponseModel()
                .addGasMetersItem(meter);
        var payload = new GetEnergyResponseModelApiDataResponse()
                .data(energy);
        var permissionRequest = new DefaultFluviusPermissionRequestBuilder()
                .permissionId("pid")
                .status(PermissionProcessStatus.VALIDATED)
                .addMeterReadings(new MeterReading("pid", "5001", null))
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