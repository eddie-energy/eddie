package energy.eddie.core.converters.v1_04;

import energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList;
import energy.eddie.core.converters.UnitConstants;
import energy.eddie.core.converters.UnsupportedUnitException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

import static energy.eddie.cim.v1_04.StandardEnergyProductTypeList.ACTIVE_ENERGY;
import static energy.eddie.cim.v1_04.StandardEnergyProductTypeList.REACTIVE_ENERGY;
import static energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList.*;


@ConditionalOnProperty(prefix = "eddie.converters", name = "energy", havingValue = "true")
@Component
class PowerToEnergyCalculation implements MeasurementCalculation {

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
    public BigDecimal convert(BigDecimal value, Duration resolution, BigDecimal scale) {
        var granularity = BigDecimal.valueOf(resolution.toMinutes())
                                    .setScale(1, RoundingMode.HALF_UP)
                                    .divide(UnitConstants.MINUTES_IN_HOUR, RoundingMode.HALF_UP);
        return value
                .setScale(1, RoundingMode.HALF_UP)
                .multiply(scale)
                .multiply(granularity);
    }

    @Override
    public ScaledUnit scaledUnit(StandardUnitOfMeasureTypeList unit) throws UnsupportedUnitException {
        return switch (unit) {
            // Apparent
            case MEGAVOLTAMPERE -> new ScaledUnit(MEGAWATT_HOURS, BigDecimal.ONE, ACTIVE_ENERGY);
            case KILOVOLT_AMPERE_REACTIVE ->
                    new ScaledUnit(MEGAVOLT_AMPERE_REACTIVE_HOURS, ONE_THOUSANDTH, REACTIVE_ENERGY);
            case MEGAVOLT_AMPERE_REACTIVE ->
                    new ScaledUnit(MEGAVOLT_AMPERE_REACTIVE_HOURS, BigDecimal.ONE, REACTIVE_ENERGY);
            // Real
            case KILOWATT -> new ScaledUnit(KILOWATT_HOUR, BigDecimal.ONE, ACTIVE_ENERGY);
            case MEGAWATT -> new ScaledUnit(MEGAWATT_HOURS, BigDecimal.ONE, ACTIVE_ENERGY);
            case WATT -> new ScaledUnit(KILOWATT_HOUR, ONE_THOUSANDTH, ACTIVE_ENERGY);
            case GIGAWATT -> new ScaledUnit(GIGAWATT_HOUR, BigDecimal.ONE, ACTIVE_ENERGY);
            default -> throw new UnsupportedUnitException();
        };
    }

    @Override
    public boolean isTargetUnit(StandardUnitOfMeasureTypeList unit) {
        return UnitConstants.ENERGY_UNITS.contains(unit);
    }
}
