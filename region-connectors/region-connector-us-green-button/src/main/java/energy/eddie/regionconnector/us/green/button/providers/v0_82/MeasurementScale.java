package energy.eddie.regionconnector.us.green.button.providers.v0_82;

import energy.eddie.cim.v0_82.vhd.UnitOfMeasureTypeList;

import java.math.BigDecimal;

public record MeasurementScale(UnitOfMeasureTypeList unit, int scale) {
    public BigDecimal scaled(long value) {
        return BigDecimal.valueOf(value)
                         .scaleByPowerOfTen(scale);
    }
}
