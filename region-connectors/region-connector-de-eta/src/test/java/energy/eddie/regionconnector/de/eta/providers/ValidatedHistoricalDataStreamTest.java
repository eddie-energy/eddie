// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.de.eta.providers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.EtaRegionConnectorMetadata;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestBuilder;
import energy.eddie.regionconnector.de.eta.permission.request.events.LatestMeterReadingEvent;
import energy.eddie.regionconnector.de.eta.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ValidatedHistoricalDataStreamTest {

    @Mock
    private Outbox outbox;

    private ValidatedHistoricalDataStream stream;

    @BeforeEach
    void setUp() {
        stream = new ValidatedHistoricalDataStream(outbox);
    }

    // ---- Energy-type enforcement ----

    @Test
    void publish_gasUnitForElectricityNeed_commitsUnfulfillable() {
        var pr = electricityRequest();
        var data = meteredData("m³", Granularity.PT15M);

        stream.publish(pr, data);

        assertUnfulfillable(pr.permissionId());
    }

    @Test
    void publish_electricityUnitForGasNeed_commitsUnfulfillable() {
        var pr = gasRequest();
        var data = meteredData("kWh", Granularity.PT15M);

        stream.publish(pr, data);

        assertUnfulfillable(pr.permissionId());
    }

    @Test
    void publish_unknownUnit_commitsUnfulfillable() {
        var pr = electricityRequest();
        var data = meteredData("BTU", Granularity.PT15M);

        stream.publish(pr, data);

        assertUnfulfillable(pr.permissionId());
    }

    // ---- Granularity enforcement ----

    @Test
    void publish_hourlyDataForQuarterHourlyNeed_commitsUnfulfillable() {
        var pr = electricityRequest(Granularity.PT15M);
        ZonedDateTime t0 = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var readings = List.of(
                reading(t0, "kWh"),
                reading(t0.plusHours(1), "kWh")   // interval = PT1H ≠ required PT15M
        );
        var data = new EtaPlusMeteredData("MP-1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), readings);

        stream.publish(pr, data);

        assertUnfulfillable(pr.permissionId());
    }

    // ---- Happy path ----

    @Test
    void publish_correctElectricityTypeAndGranularity_commitsLatestMeterReading() {
        var pr = electricityRequest(Granularity.PT15M);
        ZonedDateTime t0 = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var readings = List.of(
                reading(t0, "kWh"),
                reading(t0.plusMinutes(15), "kWh")
        );
        var data = new EtaPlusMeteredData("MP-1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), readings);

        stream.publish(pr, data);

        assertLatestMeterReadingCommitted();
    }

    @Test
    void publish_correctGasTypeAndGranularity_commitsLatestMeterReading() {
        var pr = gasRequest();
        ZonedDateTime t0 = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var readings = List.of(
                reading(t0, "m³"),
                reading(t0.plusMinutes(15), "m³")
        );
        var data = new EtaPlusMeteredData("MP-GAS", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), readings);

        stream.publish(pr, data);

        assertLatestMeterReadingCommitted();
    }

    @Test
    void publish_singleReading_granularityCannotBeInferred_passesThrough() {
        // With only one reading, granularity cannot be inferred — the data passes through
        var pr = electricityRequest(Granularity.PT15M);
        var data = new EtaPlusMeteredData("MP-1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1),
                List.of(reading(ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), "kWh")));

        stream.publish(pr, data);

        assertLatestMeterReadingCommitted();
    }

    @Test
    void publish_unrecognisedReadingInterval_granularityCannotBeInferred_passesThrough() {
        // A 7-minute interval does not match any Granularity value; the check is skipped
        var pr = electricityRequest(Granularity.PT15M);
        ZonedDateTime t0 = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var readings = List.of(reading(t0, "kWh"), reading(t0.plusMinutes(7), "kWh"));
        var data = new EtaPlusMeteredData("MP-1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), readings);

        stream.publish(pr, data);

        assertLatestMeterReadingCommitted();
    }

    @Test
    void publish_emptyReadings_passesThrough_noUnfulfillable() {
        // Empty readings are not a data-need violation (data may not be available yet)
        var pr = electricityRequest();
        var data = new EtaPlusMeteredData("MP-1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), List.of());

        stream.publish(pr, data);

        assertLatestMeterReadingCommitted();
    }

    // ---- Retransmission ----

    @Test
    void publishRetransmission_emitsToStream_withoutCommittingAnyEvent() {
        var pr = electricityRequest(Granularity.PT15M);
        ZonedDateTime t0 = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var data = new EtaPlusMeteredData("MP-1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1),
                List.of(reading(t0, "kWh"), reading(t0.plusMinutes(15), "kWh")));

        StepVerifier.create(stream.validatedHistoricalData())
                    .then(() -> stream.publishRetransmission(pr, data))
                    .assertNext(emitted -> {
                        assertThat(emitted.permissionRequest()).isEqualTo(pr);
                        assertThat(emitted.payload()).isEqualTo(data);
                    })
                    .thenCancel()
                    .verify();

        // A retransmission must not regress the watermark or change permission status.
        verifyNoInteractions(outbox);
    }

    @Test
    void publishRetransmission_doesNotEnforceDataNeedConstraints() {
        // Wrong commodity unit would mark UNFULFILLABLE via publish(); retransmission must not.
        var pr = electricityRequest(Granularity.PT15M);
        var data = meteredData("m³", Granularity.PT15M);

        stream.publishRetransmission(pr, data);

        verifyNoInteractions(outbox);
    }

    // ---- Helpers ----

    private void assertUnfulfillable(String permissionId) {
        ArgumentCaptor<PermissionEvent> captor = ArgumentCaptor.forClass(PermissionEvent.class);
        verify(outbox, times(1)).commit(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(SimpleEvent.class);
        SimpleEvent event = (SimpleEvent) captor.getValue();
        assertThat(event.permissionId()).isEqualTo(permissionId);
        assertThat(event.status()).isEqualTo(PermissionProcessStatus.UNFULFILLABLE);
    }

    private void assertLatestMeterReadingCommitted() {
        ArgumentCaptor<PermissionEvent> captor = ArgumentCaptor.forClass(PermissionEvent.class);
        verify(outbox, times(1)).commit(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(LatestMeterReadingEvent.class);
    }

    private DePermissionRequest electricityRequest() {
        return electricityRequest(Granularity.PT15M);
    }

    private DePermissionRequest electricityRequest(Granularity granularity) {
        return new DePermissionRequestBuilder()
                .permissionId("perm-elec")
                .connectionId("conn-1")
                .meteringPointId("MP-1")
                .start(LocalDate.of(2026, 1, 1))
                .end(LocalDate.of(2026, 1, 31))
                .granularity(granularity)
                .energyType(EnergyType.ELECTRICITY)
                .status(PermissionProcessStatus.ACCEPTED)
                .created(ZonedDateTime.now(EtaRegionConnectorMetadata.DE_ZONE_ID))
                .dataNeedId("need-1")
                .build();
    }

    private DePermissionRequest gasRequest() {
        return new DePermissionRequestBuilder()
                .permissionId("perm-gas")
                .connectionId("conn-gas")
                .meteringPointId("MP-GAS")
                .start(LocalDate.of(2026, 1, 1))
                .end(LocalDate.of(2026, 1, 31))
                .granularity(Granularity.PT15M)
                .energyType(EnergyType.NATURAL_GAS)
                .status(PermissionProcessStatus.ACCEPTED)
                .created(ZonedDateTime.now(EtaRegionConnectorMetadata.DE_ZONE_ID))
                .dataNeedId("need-gas")
                .build();
    }

    private static EtaPlusMeteredData meteredData(String unit, Granularity granularity) {
        ZonedDateTime t0 = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        return new EtaPlusMeteredData("MP-1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1),
                List.of(reading(t0, unit), reading(t0.plus(granularity.duration()), unit)));
    }

    private static EtaPlusMeteredData.MeterReading reading(ZonedDateTime ts, String unit) {
        return new EtaPlusMeteredData.MeterReading(ts, 1.0, unit, "VALIDATED", "Consumption");
    }
}