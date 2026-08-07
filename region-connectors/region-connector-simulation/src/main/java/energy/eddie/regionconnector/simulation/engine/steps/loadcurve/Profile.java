// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps.loadcurve;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

public class Profile {
    private final List<BigDecimal> normalizedValues;

    public Profile(List<BigDecimal> normalizedValues) {
        this.normalizedValues = normalizedValues;
    }

    public BigDecimal get(Duration loopingIndex, BigDecimal maxValue) {
        return get(loopingIndex).multiply(maxValue);
    }

    private BigDecimal get(Duration loopingIndex) {
        var minuteOfDay = loopingIndex.toMinutes() % Duration.ofDays(1).toMinutes();
        var betweenIndex = BigDecimal.valueOf(minuteOfDay).divide(durationBetweenPoints(), 10, RoundingMode.HALF_UP);
        var upperIndex = betweenIndex.setScale(0, RoundingMode.CEILING)
                                     .toBigInteger()
                                     .mod(BigInteger.valueOf(this.normalizedValues.size()));
        var lowerIndex = betweenIndex.setScale(0, RoundingMode.FLOOR);
        var dist = betweenIndex.subtract(lowerIndex);
        return interpolate(lowerIndex.toBigInteger(), upperIndex, dist);
    }

    private BigDecimal interpolate(BigInteger lower, BigInteger upper, BigDecimal lowerDistance) {
        if (lower.equals(upper)) {
            return normalizedValues.get(lower.intValue());
        }
        var lowerVal = normalizedValues.get(lower.intValue());
        var upperVal = normalizedValues.get(upper.intValue());
        return lowerVal.add(upperVal.subtract(lowerVal).multiply(lowerDistance));
    }

    private BigDecimal durationBetweenPoints() {
        var numberOfPoints = BigDecimal.valueOf(normalizedValues.size());
        var minutesInDay = Duration.ofDays(1).toMinutes();
        var oneDay = BigDecimal.valueOf(minutesInDay);
        return oneDay.divide(numberOfPoints, 1, RoundingMode.HALF_UP);
    }
}
