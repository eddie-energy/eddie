package energy.eddie.regionconnector.us.green.button.providers;

import java.math.BigDecimal;

public record MeasurementScale<U>(U unit, int scale) {
    public BigDecimal scaled(long value) {
        return BigDecimal.valueOf(value)
                .scaleByPowerOfTen(scale);
    }
}