// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers.vhd;

import energy.eddie.regionconnector.shared.cim.CimCodes;
import energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CdsMeteringSeriesTest {
    static Stream<Arguments> testUnit_returnsCorrectUnit() {
        return Stream.of(
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.KWH_NET,
                             CimCodes.UnitOfMeasure.KILOWATT_HOUR),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.DEMAND_KW,
                             CimCodes.UnitOfMeasure.KILOWATT),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.GAS_MMBTU,
                             CimCodes.UnitOfMeasure.KILOWATT_HOUR),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.GAS_MCF,
                             CimCodes.UnitOfMeasure.KILOWATT_HOUR),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.GAS_CCF,
                             CimCodes.UnitOfMeasure.KILOWATT_HOUR),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.GAS_THERM,
                             CimCodes.UnitOfMeasure.KILOWATT_HOUR),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.WATER_FT3,
                             CimCodes.UnitOfMeasure.CUBIC_METRE),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.WATER_GAL,
                             CimCodes.UnitOfMeasure.CUBIC_METRE),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.WATER_M3,
                             CimCodes.UnitOfMeasure.CUBIC_METRE),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.SUPPLY_MIX,
                             CimCodes.UnitOfMeasure.ONE),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.SUPPLY_MIX,
                             CimCodes.UnitOfMeasure.ONE)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testUnit_returnsCorrectUnit(
            UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum format,
            String expected
    ) {
        // Given
        // When
        var res = CdsMeteringSeries.unit(format);

        // Then
        assertEquals(expected, res);
    }
}