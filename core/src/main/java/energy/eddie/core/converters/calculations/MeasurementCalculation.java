package energy.eddie.core.converters.calculations;

import energy.eddie.cim.v0_82.vhd.UnitOfMeasureTypeList;
import energy.eddie.core.converters.UnsupportedUnitException;

import java.math.BigDecimal;

public interface MeasurementCalculation {
    BigDecimal convert(BigDecimal value, String resolution, BigDecimal scale);

    ScaledUnit scaledUnit(UnitOfMeasureTypeList unit) throws UnsupportedUnitException;

    boolean isTargetUnit(UnitOfMeasureTypeList unit);
}
