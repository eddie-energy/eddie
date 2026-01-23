// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.services.historical.collection;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.us.green.button.GreenButtonPermissionRequestBuilder;
import energy.eddie.regionconnector.us.green.button.client.dtos.MeterListing;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.Exports;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.Meter;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.MeterBlock;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.OngoingMonitoring;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataNeedMatcherTest {
    public static final GreenButtonPermissionRequest PERMISSION_REQUEST =
            new GreenButtonPermissionRequestBuilder().setPermissionId("pid")
                                                     .setDataNeedId("dnid")
                                                     .build();
    private final Meter meter = new Meter(
            "uid",
            "1111",
            ZonedDateTime.now(ZoneOffset.UTC),
            "mail@mail.com",
            "userId",
            false,
            false,
            false,
            List.of(),
            "status",
            "",
            ZonedDateTime.now(ZoneOffset.UTC),
            new OngoingMonitoring("", null, null, null, null),
            "DEMO-UTILITY",
            0,
            List.of(),
            List.of(),
            0,
            List.of(),
            List.of(),
            new Exports(null, null, null, null, null),
            List.of(),
            List.of(),
            List.of(),
            List.of(), List.of());
    @Mock
    private DataNeedsService dataNeedsService;
    @Mock
    private UsPermissionRequestRepository repository;
    @InjectMocks
    private DataNeedMatcher dataNeedMatcher;

    public static Stream<Arguments> filterMetersNotMeetingDataNeedCriteria_doesNotRemoveMetersMatchingCriteria() {
        return Stream.of(
                Arguments.of(EnergyType.ELECTRICITY, "electric"),
                Arguments.of(EnergyType.NATURAL_GAS, "gas"),
                Arguments.of(EnergyType.HYDROGEN, "water"),
                Arguments.of(EnergyType.HEAT, "heat")
        );
    }

    @ParameterizedTest
    @MethodSource
    void filterMetersNotMeetingDataNeedCriteria_doesNotRemoveMetersMatchingCriteria(
            EnergyType energyType,
            String serviceClass
    ) {
        // Given
        meter.setMeterBlock("base", new MeterBlock("id", "tariff", serviceClass, "", List.of(), "", "", ""));
        var data = new MeterListing(List.of(meter), null);
        when(repository.findByAuthUid("1111"))
                .thenReturn(PERMISSION_REQUEST);
        when(dataNeedsService.getById("dnid"))
                .thenReturn(
                        new ValidatedHistoricalDataDataNeed(
                                new RelativeDuration(Period.ofDays(-10), Period.ofDays(-1), null),
                                energyType,
                                Granularity.PT15M,
                                Granularity.P1D
                        )
                );

        // When
        var res = dataNeedMatcher.filterMetersNotMeetingDataNeedCriteria(data);

        // Then
        assertEquals(List.of(meter), res);
    }

    @Test
    void filterMetersNotMeetingDataNeedCriteria_doesNotRemoveMeter_whereOneMeterBlockMatchesCriteria() {
        // Given
        meter.setMeterBlock("electric", new MeterBlock("id", "tariff", "electric", "", List.of(), "", "", ""));
        meter.setMeterBlock("water", new MeterBlock("id", "tariff", "water", "", List.of(), "", "", ""));
        var data = new MeterListing(List.of(meter), null);
        when(repository.findByAuthUid("1111"))
                .thenReturn(PERMISSION_REQUEST);
        when(dataNeedsService.getById("dnid"))
                .thenReturn(
                        new ValidatedHistoricalDataDataNeed(
                                new RelativeDuration(Period.ofDays(-10), Period.ofDays(-1), null),
                                EnergyType.ELECTRICITY,
                                Granularity.PT15M,
                                Granularity.P1D
                        )
                );

        // When
        var res = dataNeedMatcher.filterMetersNotMeetingDataNeedCriteria(data);

        // Then
        assertEquals(List.of(meter), res);
    }

    @Test
    void filterMetersNotMeetingDataNeedCriteria_removesOneNotMatchingMeter() {
        // Given
        var waterMeter = new Meter(
                "uid2",
                "1111",
                ZonedDateTime.now(ZoneOffset.UTC),
                "mail@mail.com",
                "userId",
                false,
                false,
                false,
                List.of(),
                "status",
                "",
                ZonedDateTime.now(ZoneOffset.UTC),
                new OngoingMonitoring("", null, null, null, null),
                "DEMO-UTILITY",
                0,
                List.of(),
                List.of(),
                0,
                List.of(),
                List.of(),
                new Exports(null, null, null, null, null),
                List.of(),
                List.of(),
                List.of(),
                List.of(), List.of());
        meter.setMeterBlock("electric", new MeterBlock("id", "tariff", "electric", "", List.of(), "", "", ""));
        waterMeter.setMeterBlock("water", new MeterBlock("id", "tariff", "water", "", List.of(), "", "", ""));
        var data = new MeterListing(List.of(meter, waterMeter), null);
        when(repository.findByAuthUid("1111"))
                .thenReturn(PERMISSION_REQUEST);
        when(dataNeedsService.getById("dnid"))
                .thenReturn(
                        new ValidatedHistoricalDataDataNeed(
                                new RelativeDuration(Period.ofDays(-10), Period.ofDays(-1), null),
                                EnergyType.ELECTRICITY,
                                Granularity.PT15M,
                                Granularity.P1D
                        )
                );

        // When
        var res = dataNeedMatcher.filterMetersNotMeetingDataNeedCriteria(data);

        // Then
        assertEquals(List.of(meter), res);
    }

    @Test
    void filterMetersNotMeetingDataNeedCriteria_removesMeter_withoutMatchingMeterBlock() {
        // Given
        meter.setMeterBlock("water", new MeterBlock("id", "tariff", "water", "", List.of(), "", "", ""));
        var data = new MeterListing(List.of(meter), null);
        when(repository.findByAuthUid("1111"))
                .thenReturn(PERMISSION_REQUEST);
        when(dataNeedsService.getById("dnid"))
                .thenReturn(
                        new ValidatedHistoricalDataDataNeed(
                                new RelativeDuration(Period.ofDays(-10), Period.ofDays(-1), null),
                                EnergyType.ELECTRICITY,
                                Granularity.PT15M,
                                Granularity.P1D
                        )
                );

        // When
        var res = dataNeedMatcher.filterMetersNotMeetingDataNeedCriteria(data);

        // Then
        assertThat(res).isEmpty();
    }

    @Test
    void filterMetersNotMeetingDataNeedCriteria_removesMeter_withWrongDataNeed() {
        // Given
        meter.setMeterBlock("water", new MeterBlock("id", "tariff", "water", "", List.of(), "", "", ""));
        var data = new MeterListing(List.of(meter), null);
        when(repository.findByAuthUid("1111"))
                .thenReturn(PERMISSION_REQUEST);
        when(dataNeedsService.getById("dnid"))
                .thenReturn(new OutboundAiidaDataNeed());

        // When
        var res = dataNeedMatcher.filterMetersNotMeetingDataNeedCriteria(data);

        // Then
        assertThat(res).isEmpty();
    }
}