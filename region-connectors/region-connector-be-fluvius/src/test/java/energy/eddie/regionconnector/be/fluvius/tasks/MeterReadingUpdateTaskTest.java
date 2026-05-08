// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.tasks;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.ApiMetaData;
import energy.eddie.regionconnector.be.fluvius.client.model.v3.energy.*;
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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.*;

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
        var item = new MeasurementSlice(null, ZonedDateTime.now(ZoneOffset.UTC), null);
        var meter = new PhysicalMeter(null, "002", null, null, List.of(item));
        var energy = new GetEnergyResponseModel(new MeteringOnMeter("5002", EnergyType.ELECTRICITY, List.of(meter)));
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
        var payload = new GetEnergyResponseModelApiDataResponse(
                new ApiMetaData(null),
                new GetEnergyResponseModel(null)
        );
        var permissionRequest = new DefaultFluviusPermissionRequestBuilder()
                .permissionId("pid")
                .addMeterReadings(new MeterReading("pid", "5001", null))
                .build();
        when(meterReadingRepository.findAllByPermissionId("pid")).thenReturn(permissionRequest.lastMeterReadings());

        // When
        streams.publish(permissionRequest, payload);

        // Then
        verify(meterReadingRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void testOnMeterReading_whereQuarterHourlyAndDailyValuesAreAvailableForTheSameMeter_updatesCorrectMeterReading() {
        // Given
        var end = ZonedDateTime.of(2025, 1, 31, 0, 0, 0, 0, ZoneOffset.UTC);
        var newEnd = ZonedDateTime.of(2025, 2, 20, 0, 0, 0, 0, ZoneOffset.UTC);
        var quarterHourlyItem = new MeasurementSlice(null, newEnd, null);
        var meter1 = new PhysicalMeter(null, "001", null, null, List.of(quarterHourlyItem));
        var dailyItem = new MeasurementSlice(null, null, null);
        var meter2 = new PhysicalMeter(null, "001", List.of(dailyItem), null, null);
        var energy = new GetEnergyResponseModel(new MeteringOnMeter("5001",
                                                                    EnergyType.ELECTRICITY,
                                                                    List.of(meter1, meter2)));
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
        var end = ZonedDateTime.of(2025, 1, 31, 0, 0, 0, 0, ZoneOffset.UTC);
        var meter = new PhysicalMeter(null, "001", null, null, null);
        var energy = new GetEnergyResponseModel(new MeteringOnMeter("5001", EnergyType.ELECTRICITY, List.of(meter)));
        var payload = new GetEnergyResponseModelApiDataResponse(null, energy);
        var permissionRequest = new DefaultFluviusPermissionRequestBuilder()
                .permissionId("pid")
                .addMeterReadings(new MeterReading("pid", "5001", end))
                .build();
        when(meterReadingRepository.findAllByPermissionId("pid")).thenReturn(permissionRequest.lastMeterReadings());

        // When
        streams.publish(permissionRequest, payload);

        // Then
        verify(meterReadingRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void testOnMeterReading_forGasMeters_updatesCorrectMeterReading() {
        // Given
        var end = ZonedDateTime.of(2025, 1, 31, 0, 0, 0, 0, ZoneOffset.UTC);
        var item1 = new MeasurementSlice(null, end.minusDays(1), null);
        var item2 = new MeasurementSlice(null, end, null);
        var item3 = new MeasurementSlice(null, end.minusDays(2), null);
        var meter = new PhysicalMeter(null, "001", null, List.of(item1, item2, item3), null);
        var energy = new GetEnergyResponseModel(new MeteringOnMeter("5001", EnergyType.GAS, List.of(meter)));
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
        var item = new MeasurementSlice(null, end, null);
        var meter = new PhysicalMeter(null, "001", null, List.of(item), null);
        var energy = new GetEnergyResponseModel(new MeteringOnMeter("5001", EnergyType.ELECTRICITY, List.of(meter)));
        var payload = new GetEnergyResponseModelApiDataResponse(null, energy);
        var meterReading = new MeterReading("pid", "5001", end.minusDays(1));
        var permissionRequest = new DefaultFluviusPermissionRequestBuilder()
                .permissionId("pid")
                .status(PermissionProcessStatus.VALIDATED)
                .addMeterReadings(meterReading)
                .granularity(Granularity.PT1H)
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