package energy.eddie.core.converters.v1_04;

import energy.eddie.cim.v1_04.StandardUnitOfMeasureTypeList;
import energy.eddie.core.converters.UnsupportedUnitException;

import java.math.BigDecimal;
import java.time.Duration;

interface MeasurementCalculation {
    BigDecimal convert(BigDecimal value, Duration resolution, BigDecimal scale);

    ScaledUnit scaledUnit(StandardUnitOfMeasureTypeList unit) throws UnsupportedUnitException;

    boolean isTargetUnit(StandardUnitOfMeasureTypeList unit);
}
