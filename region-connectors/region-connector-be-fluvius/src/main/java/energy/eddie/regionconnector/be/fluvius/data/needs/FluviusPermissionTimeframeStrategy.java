// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.data.needs;

import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.PermissionTimeframeStrategy;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;

public class FluviusPermissionTimeframeStrategy implements PermissionTimeframeStrategy {

    @Override
    public Timeframe permissionTimeframe(
            @Nullable Timeframe energyDataTimeframe,
            @Nullable DataNeed dataNeed,
            ZonedDateTime referenceDateTime
    ) {
        var referenceDate = referenceDateTime.toLocalDate();
        if (energyDataTimeframe == null) {
            return new Timeframe(referenceDate, referenceDate.plusDays(1));
        }
        var start = energyDataTimeframe.start();
        if (start.isAfter(referenceDate)) {
            start = referenceDate;
        }
        var end = energyDataTimeframe.end();
        if (end.isBefore(referenceDate)) {
            end = referenceDate;
        }
        return new Timeframe(start, end);
    }
}
