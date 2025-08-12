package energy.eddie.core.converters.v1_04;

import energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList;
import energy.eddie.core.converters.UnitConstants;
import energy.eddie.core.converters.UnsupportedUnitException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

import static energy.eddie.cim.v1_04.StandardEnergyProductTypeList.ACTIVE_POWER;
import static energy.eddie.cim.v1_04.StandardEnergyProductTypeList.REACTIVE_POWER;
import static energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList.*;


@ConditionalOnProperty(prefix = "eddie.converters", name = "power", havingValue = "true")
@Component
class EnergyToPowerCalculation implements MeasurementCalculation {

    /**
     * Converts energy and hours to power. For more information see <a
     * href="https://www.rapidtables.com/convert/electric/wh-to-watt.html">this</a>
     *
     * @param value      the energy
     * @param resolution used to get the hours
     * @param scale      the scale that should be used for the calculation
     * @return the power for a resolution
     */
    @Override
    public BigDecimal convert(BigDecimal value, Duration resolution, BigDecimal scale) {
        var granularity = BigDecimal.valueOf(resolution.toMinutes())
                                    .setScale(1, RoundingMode.HALF_UP)
                                    .divide(UnitConstants.MINUTES_IN_HOUR, RoundingMode.HALF_UP);
        return value
                .setScale(1, RoundingMode.HALF_UP)
                .divide(granularity, RoundingMode.HALF_UP);
    }

    @Override
    public ScaledUnit scaledUnit(StandardUnitOfMeasureTypeList unit) throws UnsupportedUnitException {
        return switch (unit) {
            case KILOWATT_HOUR -> new ScaledUnit(KILOWATT, BigDecimal.ONE, ACTIVE_POWER);
            case GIGAWATT_HOUR -> new ScaledUnit(GIGAWATT, BigDecimal.ONE, ACTIVE_POWER);
            case MEGAVOLT_AMPERE_REACTIVE_HOURS -> new ScaledUnit(MEGAVOLT_AMPERE_REACTIVE, BigDecimal.ONE, REACTIVE_POWER);
            case MEGAWATT_HOURS -> new ScaledUnit(MEGAWATT, BigDecimal.ONE, ACTIVE_POWER);
            default -> throw new UnsupportedUnitException();
        };
    }

    @Override
    public boolean isTargetUnit(StandardUnitOfMeasureTypeList unit) {
        return UnitConstants.POWER_UNITS.contains(unit);
    }
}
