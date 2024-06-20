package energy.eddie.core.converters.calculations;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.cim.v0_82.vhd.EnergyProductTypeList;
import energy.eddie.cim.v0_82.vhd.UnitOfMeasureTypeList;
import energy.eddie.core.converters.UnitConstants;
import energy.eddie.core.converters.UnsupportedUnitException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@ConditionalOnProperty(prefix = "eddie.converters", name = "power", havingValue = "true")
@Component
public class EnergyToPowerCalculation implements MeasurementCalculation {

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
    public BigDecimal convert(BigDecimal value, String resolution, BigDecimal scale) {
        var granularity = BigDecimal.valueOf(Granularity.valueOf(resolution).minutes())
                                    .setScale(1, RoundingMode.HALF_UP)
                                    .divide(UnitConstants.MINUTES_IN_HOUR, RoundingMode.HALF_UP);
        return value
                .setScale(1, RoundingMode.HALF_UP)
                .divide(granularity, RoundingMode.HALF_UP);
    }

    @Override
    public ScaledUnit scaledUnit(UnitOfMeasureTypeList unit) throws UnsupportedUnitException {
        return switch (unit) {
            case UnitOfMeasureTypeList.KILOWATT_HOUR ->
                    new ScaledUnit(UnitOfMeasureTypeList.KILOWATT, BigDecimal.ONE, EnergyProductTypeList.ACTIVE_POWER);
            case UnitOfMeasureTypeList.GIGAWATT_HOUR ->
                    new ScaledUnit(UnitOfMeasureTypeList.GIGAWATT, BigDecimal.ONE, EnergyProductTypeList.ACTIVE_POWER);
            case UnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE_HOURS ->
                    new ScaledUnit(UnitOfMeasureTypeList.MEGAVOLT_AMPERE_REACTIVE,
                                   BigDecimal.ONE,
                                   EnergyProductTypeList.REACTIVE_POWER);
            case UnitOfMeasureTypeList.MEGAWATT_HOURS ->
                    new ScaledUnit(UnitOfMeasureTypeList.MEGAWATT, BigDecimal.ONE, EnergyProductTypeList.ACTIVE_POWER);
            default -> throw new UnsupportedUnitException();
        };
    }

    @Override
    public boolean isTargetUnit(UnitOfMeasureTypeList unit) {
        return UnitConstants.powerUnits.contains(unit);
    }
}
