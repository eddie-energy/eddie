// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.providers.vhd;

import energy.eddie.cim.v0_82.vhd.UnitOfMeasureTypeList;
import energy.eddie.regionconnector.cds.openapi.model.UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner.FormatEnum;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CimUnitConverterTest {

    static Stream<Arguments> testUnit_returnsCorrectUnit() {
        return Stream.of(
                Arguments.of(FormatEnum.KWH_NET, UnitOfMeasureTypeList.KILOWATT_HOUR),
                Arguments.of(FormatEnum.DEMAND_KW, UnitOfMeasureTypeList.KILOWATT),
                Arguments.of(FormatEnum.GAS_MMBTU, UnitOfMeasureTypeList.KILOWATT_HOUR),
                Arguments.of(FormatEnum.GAS_MCF, UnitOfMeasureTypeList.KILOWATT_HOUR),
                Arguments.of(FormatEnum.GAS_CCF, UnitOfMeasureTypeList.KILOWATT_HOUR),
                Arguments.of(FormatEnum.GAS_THERM, UnitOfMeasureTypeList.KILOWATT_HOUR),
                Arguments.of(FormatEnum.WATER_FT3, UnitOfMeasureTypeList.CUBIC_METRE),
                Arguments.of(FormatEnum.WATER_GAL, UnitOfMeasureTypeList.CUBIC_METRE),
                Arguments.of(FormatEnum.WATER_M3, UnitOfMeasureTypeList.CUBIC_METRE),
                Arguments.of(FormatEnum.SUPPLY_MIX, null),
                Arguments.of(FormatEnum.SUPPLY_MIX, null)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testUnit_returnsCorrectUnit(FormatEnum format, UnitOfMeasureTypeList expected) {
        // Given
        var converter = new CimUnitConverter(format);

        // When
        var res = converter.unit();

        // Then
        assertEquals(expected, res);
    }
}