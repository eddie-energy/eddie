// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.providers;

import java.math.BigDecimal;

public record MeasurementScale<U>(U unit, int scale) {
    public BigDecimal scaled(long value) {
        return BigDecimal.valueOf(value)
                .scaleByPowerOfTen(scale);
    }
}