// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers.vhd.v1_04;

import energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList;
import energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CimUnitConverterTest {
    static Stream<Arguments> testUnit_returnsCorrectUnit() {
        return Stream.of(
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.KWH_NET,
                             StandardUnitOfMeasureTypeList.KILOWATT_HOUR),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.DEMAND_KW,
                             StandardUnitOfMeasureTypeList.KILOWATT),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.GAS_MMBTU,
                             StandardUnitOfMeasureTypeList.KILOWATT_HOUR),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.GAS_MCF,
                             StandardUnitOfMeasureTypeList.KILOWATT_HOUR),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.GAS_CCF,
                             StandardUnitOfMeasureTypeList.KILOWATT_HOUR),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.GAS_THERM,
                             StandardUnitOfMeasureTypeList.KILOWATT_HOUR),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.WATER_FT3,
                             StandardUnitOfMeasureTypeList.CUBIC_METRE),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.WATER_GAL,
                             StandardUnitOfMeasureTypeList.CUBIC_METRE),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.WATER_M3,
                             StandardUnitOfMeasureTypeList.CUBIC_METRE),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.SUPPLY_MIX,
                             StandardUnitOfMeasureTypeList.ONE),
                Arguments.of(UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum.SUPPLY_MIX,
                             StandardUnitOfMeasureTypeList.ONE)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testUnit_returnsCorrectUnit(
            UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum format,
            StandardUnitOfMeasureTypeList expected
    ) {
        // Given
        var converter = new CimUnitConverter(format);

        // When
        var res = converter.unit();

        // Then
        assertEquals(expected, res);
    }
}