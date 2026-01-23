// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.duration.AbsoluteDuration;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.cds.openapi.model.*;
import energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.providers.vhd.IdentifiableValidatedHistoricalData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.cds.providers.vhd.IdentifiableValidatedHistoricalData.Payload;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataNeedMapperTest {
    @Mock
    private DataNeedsService dataNeedsService;
    @InjectMocks
    private DataNeedMapper dataNeedMapper;

    static Stream<Arguments> testFilter_forValidatedHistoricalDataDataNeed_returnsOnlyMatchingUsageSegments() {
        return Stream.of(
                Arguments.of(EnergyType.ELECTRICITY, FormatEnum.KWH_NET, FormatEnum.GAS_THERM),
                Arguments.of(EnergyType.NATURAL_GAS, FormatEnum.GAS_THERM, FormatEnum.WATER_M3),
                Arguments.of(EnergyType.NATURAL_GAS, FormatEnum.GAS_THERM, FormatEnum.KWH_NET)
        );
    }

    @Test
    void testFilter_forNonValidatedHistoricalDataDataNeed_returnsSame() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .build();
        var data = new IdentifiableValidatedHistoricalData(
                pr,
                new Payload(
                        List.of(new AccountsEndpoint200ResponseAllOfAccountsInner()),
                        List.of(new ServiceContractEndpoint200ResponseAllOfServiceContractsInner()),
                        List.of(new ServicePointEndpoint200ResponseAllOfServicePointsInner()),
                        List.of(new MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner()),
                        List.of(new UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner())
                )
        );
        when(dataNeedsService.getById("dnid"))
                .thenReturn(new AccountingPointDataNeed());

        // When
        var res = dataNeedMapper.apply(data);

        // Then
        assertEquals(data, res);
    }

    @ParameterizedTest
    @MethodSource
    void testFilter_forValidatedHistoricalDataDataNeed_returnsOnlyMatchingUsageSegments(
            EnergyType energyType,
            FormatEnum wanted,
            FormatEnum other
    ) {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .build();
        var eVal1 = Map.of("v", BigDecimal.TEN);
        var eVal2 = Map.of("v", BigDecimal.TWO);
        var otherValue = Map.of("v", BigDecimal.ONE);
        var data = new IdentifiableValidatedHistoricalData(
                pr,
                new Payload(
                        List.of(new AccountsEndpoint200ResponseAllOfAccountsInner()),
                        List.of(new ServiceContractEndpoint200ResponseAllOfServiceContractsInner()),
                        List.of(new ServicePointEndpoint200ResponseAllOfServicePointsInner()),
                        List.of(new MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner()),
                        List.of(new UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner()
                                        .interval(BigDecimal.valueOf(900))
                                        .addFormatItem(wanted)
                                        .addFormatItem(other)
                                        .addFormatItem(wanted)
                                        .addFormatItem(other)
                                        .addValuesItem(List.of(eVal1, otherValue, eVal2, otherValue))
                                        .addValuesItem(List.of(eVal1, otherValue, eVal2, otherValue))
                                        .addValuesItem(List.of(eVal1, otherValue, eVal2, otherValue))
                                        .addValuesItem(List.of(eVal1, otherValue, eVal2, otherValue))
                                        .addValuesItem(List.of(eVal1, otherValue, eVal2, otherValue))
                        )
                )
        );
        var expected = new UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner()
                .interval(BigDecimal.valueOf(900))
                .addFormatItem(wanted)
                .addFormatItem(wanted)
                .addValuesItem(List.of(eVal1, eVal2))
                .addValuesItem(List.of(eVal1, eVal2))
                .addValuesItem(List.of(eVal1, eVal2))
                .addValuesItem(List.of(eVal1, eVal2))
                .addValuesItem(List.of(eVal1, eVal2));
        var today = LocalDate.now(ZoneOffset.UTC);
        when(dataNeedsService.getById("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(new AbsoluteDuration(today, today),
                                                                energyType,
                                                                Granularity.PT5M,
                                                                Granularity.P1Y));

        // When
        var res = dataNeedMapper.apply(data);

        // Then
        assertEquals(List.of(expected), res.payload().usageSegments());
    }

    @Test
    void testFilter_forValidatedHistoricalDataDataNeedWhereNothingMatches_returnsEmpty() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .build();
        var value = Map.of("v", BigDecimal.TEN);
        var data = new IdentifiableValidatedHistoricalData(
                pr,
                new Payload(
                        List.of(new AccountsEndpoint200ResponseAllOfAccountsInner()),
                        List.of(new ServiceContractEndpoint200ResponseAllOfServiceContractsInner()),
                        List.of(new ServicePointEndpoint200ResponseAllOfServicePointsInner()),
                        List.of(new MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner()),
                        List.of(new UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner()
                                        .interval(BigDecimal.valueOf(900))
                                        .addFormatItem(FormatEnum.WATER_M3)
                                        .addValuesItem(List.of(value))
                                        .addValuesItem(List.of(value))
                                        .addValuesItem(List.of(value))
                                        .addValuesItem(List.of(value))
                                        .addValuesItem(List.of(value))
                        )
                )
        );
        var today = LocalDate.now(ZoneOffset.UTC);
        when(dataNeedsService.getById("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(new AbsoluteDuration(today, today),
                                                                EnergyType.ELECTRICITY,
                                                                Granularity.PT5M,
                                                                Granularity.P1Y));

        // When
        var res = dataNeedMapper.apply(data);

        // Then
        assertThat(res.payload().usageSegments()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {300, 60})
    void testFilter_forInvalidGranularity_returnsEmpty(int interval) {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setDataNeedId("dnid")
                .build();
        var value = Map.of("v", BigDecimal.TEN);
        var data = new IdentifiableValidatedHistoricalData(
                pr,
                new Payload(
                        List.of(new AccountsEndpoint200ResponseAllOfAccountsInner()),
                        List.of(new ServiceContractEndpoint200ResponseAllOfServiceContractsInner()),
                        List.of(new ServicePointEndpoint200ResponseAllOfServicePointsInner()),
                        List.of(new MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner()),
                        List.of(new UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner()
                                        .interval(BigDecimal.valueOf(interval))
                                        .addFormatItem(FormatEnum.KWH_FWD)
                                        .addValuesItem(List.of(value))
                                        .addValuesItem(List.of(value))
                                        .addValuesItem(List.of(value))
                                        .addValuesItem(List.of(value))
                                        .addValuesItem(List.of(value))
                        )
                )
        );
        var today = LocalDate.now(ZoneOffset.UTC);
        when(dataNeedsService.getById("dnid"))
                .thenReturn(new ValidatedHistoricalDataDataNeed(new AbsoluteDuration(today, today),
                                                                EnergyType.ELECTRICITY,
                                                                Granularity.PT15M,
                                                                Granularity.P1Y));

        // When
        var res = dataNeedMapper.apply(data);

        // Then
        assertThat(res.payload().usageSegments()).isEmpty();
    }
}