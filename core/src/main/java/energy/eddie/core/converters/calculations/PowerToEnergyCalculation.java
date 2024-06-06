package energy.eddie.core.converters.calculations;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.cim.v0_82.vhd.EnergyProductTypeList;
import energy.eddie.cim.v0_82.vhd.UnitOfMeasureTypeList;
import energy.eddie.core.converters.UnitConstants;
import energy.eddie.core.converters.UnsupportedUnitException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;

@ConditionalOnProperty(prefix = "eddie.converters", name = "energy", havingValue = "true")
@Component
public class PowerToEnergyCalculation implements MeasurementCalculation {

    // 0.001
    private static final BigDecimal ONE_THOUSANDTH = BigDecimal.valueOf(1, 3);

    /**
     * Converts energy and hours to power. For more information see <a
     * href="https://www.rapidtables.com/convert/electric/wh-to-watt.html">this</a>
     *
     * @param value      the power
     * @param resolution used to get the hours
     * @param scale      the scale that should be used for the calculation
     * @return the energy for a resolution
     */
    @Override
    public BigDecimal convert(BigDecimal value, String resolution, BigDecimal scale) {
        var hours = BigDecimal.valueOf(
                Duration.ofMinutes(
                                Granularity.valueOf(resolution)
                                           .minutes()
                        )
                        .toHours()
        );
        return value.multiply(scale).multiply(hours);
    }

    @Override
    public ScaledUnit scaledUnit(UnitOfMeasureTypeList unit) throws UnsupportedUnitException {
        return switch (unit) {
            case UnitOfMeasureTypeList.KILOWATT -> new ScaledUnit(UnitOfMeasureTypeList.KILOWATT_HOUR,
                                                                  BigDecimal.ONE,
                                                                  EnergyProductTypeList.ACTIVE_ENERGY);
            case UnitOfMeasureTypeList.GIGAWATT -> new ScaledUnit(UnitOfMeasureTypeList.GIGAWATT_HOUR,
                                                                  BigDecimal.ONE,
                                                                  EnergyProductTypeList.ACTIVE_ENERGY);
            case UnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE ->
                    new ScaledUnit(UnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE_HOURS,
                                   BigDecimal.ONE,
                                   EnergyProductTypeList.REACTIVE_ENERGY);
            case UnitOfMeasureTypeList.KILOVOLT_AMPERE_REACTIVE ->
                    new ScaledUnit(UnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE_HOURS,
                                   ONE_THOUSANDTH,
                                   EnergyProductTypeList.REACTIVE_ENERGY);
            case UnitOfMeasureTypeList.MEGAWATT -> new ScaledUnit(UnitOfMeasureTypeList.MEGAWATT_HOURS,
                                                                  BigDecimal.ONE,
                                                                  EnergyProductTypeList.ACTIVE_ENERGY);
            case UnitOfMeasureTypeList.WATT -> new ScaledUnit(UnitOfMeasureTypeList.KILOWATT_HOUR,
                                                              ONE_THOUSANDTH,
                                                              EnergyProductTypeList.ACTIVE_ENERGY);
            default -> throw new UnsupportedUnitException();
        };
    }

    @Override
    public boolean isTargetUnit(UnitOfMeasureTypeList unit) {
        return UnitConstants.energyUnits.contains(unit);
    }
}
