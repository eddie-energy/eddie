// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.cim;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Ensures that the version-neutral codes match the code lists of every supported CIM version.
 */
class CimCodesTest {
    @ParameterizedTest
    @MethodSource("codes")
    void code_matchesV104AndV112(String code, String v104, String v112) {
        assertEquals(v104, code);
        assertEquals(v112, code);
    }

    private static Stream<Arguments> codes() {
        return Stream.of(
                Arguments.of(CimCodes.Direction.UP, energy.eddie.cim.v1_04.StandardDirectionTypeList.UP.value(), energy.eddie.cim.v1_12.StandardDirectionTypeList.UP.value()),
                Arguments.of(CimCodes.Direction.DOWN, energy.eddie.cim.v1_04.StandardDirectionTypeList.DOWN.value(), energy.eddie.cim.v1_12.StandardDirectionTypeList.DOWN.value()),
                Arguments.of(CimCodes.Direction.UP_AND_DOWN, energy.eddie.cim.v1_04.StandardDirectionTypeList.UP_AND_DOWN.value(), energy.eddie.cim.v1_12.StandardDirectionTypeList.UP_AND_DOWN.value()),
                Arguments.of(CimCodes.Quality.ADJUSTED, energy.eddie.cim.v1_04.StandardQualityTypeList.ADJUSTED.value(), energy.eddie.cim.v1_12.StandardQualityTypeList.ADJUSTED.value()),
                Arguments.of(CimCodes.Quality.NOT_AVAILABLE, energy.eddie.cim.v1_04.StandardQualityTypeList.NOT_AVAILABLE.value(), energy.eddie.cim.v1_12.StandardQualityTypeList.NOT_AVAILABLE.value()),
                Arguments.of(CimCodes.Quality.ESTIMATED, energy.eddie.cim.v1_04.StandardQualityTypeList.ESTIMATED.value(), energy.eddie.cim.v1_12.StandardQualityTypeList.ESTIMATED.value()),
                Arguments.of(CimCodes.Quality.AS_PROVIDED, energy.eddie.cim.v1_04.StandardQualityTypeList.AS_PROVIDED.value(), energy.eddie.cim.v1_12.StandardQualityTypeList.AS_PROVIDED.value()),
                Arguments.of(CimCodes.BusinessType.PRODUCTION, energy.eddie.cim.v1_04.StandardBusinessTypeList.PRODUCTION.value(), energy.eddie.cim.v1_12.StandardBusinessTypeList.PRODUCTION.value()),
                Arguments.of(CimCodes.BusinessType.CONSUMPTION, energy.eddie.cim.v1_04.StandardBusinessTypeList.CONSUMPTION.value(), energy.eddie.cim.v1_12.StandardBusinessTypeList.CONSUMPTION.value()),
                Arguments.of(CimCodes.BusinessType.NET_PRODUCTION_CONSUMPTION, energy.eddie.cim.v1_04.StandardBusinessTypeList.NET_PRODUCTION__CONSUMPTION.value(), energy.eddie.cim.v1_12.StandardBusinessTypeList.NET_PRODUCTION__CONSUMPTION.value()),
                Arguments.of(CimCodes.EnergyProduct.ACTIVE_POWER, energy.eddie.cim.v1_04.StandardEnergyProductTypeList.ACTIVE_POWER.value(), energy.eddie.cim.v1_12.StandardEnergyProductTypeList.ACTIVE_POWER.value()),
                Arguments.of(CimCodes.EnergyProduct.ACTIVE_ENERGY, energy.eddie.cim.v1_04.StandardEnergyProductTypeList.ACTIVE_ENERGY.value(), energy.eddie.cim.v1_12.StandardEnergyProductTypeList.ACTIVE_ENERGY.value()),
                Arguments.of(CimCodes.EnergyProduct.REACTIVE_ENERGY, energy.eddie.cim.v1_04.StandardEnergyProductTypeList.REACTIVE_ENERGY.value(), energy.eddie.cim.v1_12.StandardEnergyProductTypeList.REACTIVE_ENERGY.value()),
                Arguments.of(CimCodes.EnergyProduct.CAPACITIVE_REACTIVE_ENERGY, energy.eddie.cim.v1_04.StandardEnergyProductTypeList.CAPACITIVE_REACTIVE_ENERGY.value(), energy.eddie.cim.v1_12.StandardEnergyProductTypeList.CAPACITIVE_REACTIVE_ENERGY.value()),
                Arguments.of(CimCodes.EnergyProduct.INDUCTIVE_REACTIVE_ENERGY, energy.eddie.cim.v1_04.StandardEnergyProductTypeList.INDUCTIVE_REACTIVE_ENERGY.value(), energy.eddie.cim.v1_12.StandardEnergyProductTypeList.INDUCTIVE_REACTIVE_ENERGY.value()),
                Arguments.of(CimCodes.UnitOfMeasure.AMPERE, energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList.AMPERE.value(), energy.eddie.cim.v1_12.StandardUnitOfMeasureTypeList.AMPERE.value()),
                Arguments.of(CimCodes.UnitOfMeasure.CELSIUS, energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList.CELSIUS.value(), energy.eddie.cim.v1_12.StandardUnitOfMeasureTypeList.CELSIUS.value()),
                Arguments.of(CimCodes.UnitOfMeasure.CUBIC_METRE, energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList.CUBIC_METRE.value(), energy.eddie.cim.v1_12.StandardUnitOfMeasureTypeList.CUBIC_METRE.value()),
                Arguments.of(CimCodes.UnitOfMeasure.CUBIC_METRES_PER_SECOND, energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList.CUBIC_METRES_PER_SECOND.value(), energy.eddie.cim.v1_12.StandardUnitOfMeasureTypeList.CUBIC_METRES_PER_SECOND.value()),
                Arguments.of(CimCodes.UnitOfMeasure.DEGREE_UNIT_OF_ANGLE, energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList.DEGREE_UNIT_OF_ANGLE.value(), energy.eddie.cim.v1_12.StandardUnitOfMeasureTypeList.DEGREE_UNIT_OF_ANGLE.value()),
                Arguments.of(CimCodes.UnitOfMeasure.HECTOPASCAL, energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList.HECTOPASCAL.value(), energy.eddie.cim.v1_12.StandardUnitOfMeasureTypeList.HECTOPASCAL.value()),
                Arguments.of(CimCodes.UnitOfMeasure.KELVIN, energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList.K_KELVIN.value(), energy.eddie.cim.v1_12.StandardUnitOfMeasureTypeList.K_KELVIN.value()),
                Arguments.of(CimCodes.UnitOfMeasure.KILOVOLT, energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList.KILOVOLT.value(), energy.eddie.cim.v1_12.StandardUnitOfMeasureTypeList.KILOVOLT.value()),
                Arguments.of(CimCodes.UnitOfMeasure.KILOVOLT_AMPERE_REACTIVE, energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList.KILOVOLT_AMPERE_REACTIVE.value(), energy.eddie.cim.v1_12.StandardUnitOfMeasureTypeList.KILOVOLT_AMPERE_REACTIVE.value()),
                Arguments.of(CimCodes.UnitOfMeasure.KILOWATT, energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList.KILOWATT.value(), energy.eddie.cim.v1_12.StandardUnitOfMeasureTypeList.KILOWATT.value()),
                Arguments.of(CimCodes.UnitOfMeasure.KILOWATT_HOUR, energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList.KILOWATT_HOUR.value(), energy.eddie.cim.v1_12.StandardUnitOfMeasureTypeList.KILOWATT_HOUR.value()),
                Arguments.of(CimCodes.UnitOfMeasure.MEGAVOLT_AMPERE, energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList.MEGAVOLTAMPERE.value(), energy.eddie.cim.v1_12.StandardUnitOfMeasureTypeList.MEGAVOLTAMPERE.value()),
                Arguments.of(CimCodes.UnitOfMeasure.MEGAVOLT_AMPERE_REACTIVE_HOURS, energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE_HOURS.value(), energy.eddie.cim.v1_12.StandardUnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE_HOURS.value()),
                Arguments.of(CimCodes.UnitOfMeasure.MEGAWATT_HOURS, energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList.MEGAWATT_HOURS.value(), energy.eddie.cim.v1_12.StandardUnitOfMeasureTypeList.MEGAWATT_HOURS.value()),
                Arguments.of(CimCodes.UnitOfMeasure.METRE, energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList.METRE.value(), energy.eddie.cim.v1_12.StandardUnitOfMeasureTypeList.METRE.value()),
                Arguments.of(CimCodes.UnitOfMeasure.ONE, energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList.ONE.value(), energy.eddie.cim.v1_12.StandardUnitOfMeasureTypeList.ONE.value()),
                Arguments.of(CimCodes.UnitOfMeasure.WATT, energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList.WATT.value(), energy.eddie.cim.v1_12.StandardUnitOfMeasureTypeList.WATT.value()),
                Arguments.of(CimCodes.ReasonCode.ERRORS_NOT_SPECIFICALLY_IDENTIFIED, energy.eddie.cim.v1_04.StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value(), energy.eddie.cim.v1_12.StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value()),
                Arguments.of(CimCodes.Commodity.NONE, energy.eddie.cim.v1_04.vhd.CommodityKind.NONE.value(), energy.eddie.cim.v1_12.vhd.CommodityKind._0.value()),
                Arguments.of(CimCodes.Commodity.ELECTRICITY_SECONDARY_METERED, energy.eddie.cim.v1_04.vhd.CommodityKind.ELECTRICITYSECONDARYMETERED.value(), energy.eddie.cim.v1_12.vhd.CommodityKind._1.value()),
                Arguments.of(CimCodes.Commodity.ELECTRICITY_PRIMARY_METERED, energy.eddie.cim.v1_04.vhd.CommodityKind.ELECTRICITYPRIMARYMETERED.value(), energy.eddie.cim.v1_12.vhd.CommodityKind._2.value()),
                Arguments.of(CimCodes.Commodity.NATURAL_GAS, energy.eddie.cim.v1_04.vhd.CommodityKind.NATURALGAS.value(), energy.eddie.cim.v1_12.vhd.CommodityKind._7.value()),
                Arguments.of(CimCodes.Commodity.POTABLE_WATER, energy.eddie.cim.v1_04.vhd.CommodityKind.POTABLEWATER.value(), energy.eddie.cim.v1_12.vhd.CommodityKind._9.value()),
                Arguments.of(CimCodes.Aggregate.SUM, energy.eddie.cim.v1_04.vhd.AggregateKind.SUM.value(), energy.eddie.cim.v1_12.vhd.AggregateKind._26.value())
        );
    }
}
