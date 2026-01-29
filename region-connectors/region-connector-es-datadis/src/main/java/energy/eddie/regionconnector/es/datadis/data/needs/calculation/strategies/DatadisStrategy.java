// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.data.needs.calculation.strategies;

import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.PermissionTimeframeStrategy;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;

public class DatadisStrategy implements PermissionTimeframeStrategy {
    @Override
    public Timeframe permissionTimeframe(@Nullable Timeframe energyDataTimeframe, ZonedDateTime referenceDateTime) {
        var now = referenceDateTime.toLocalDate();
        if (energyDataTimeframe == null || !now.isBefore(energyDataTimeframe.end())) {
            // if all the data is in the past, or it's accounting point data, we only need access for 1 day
            return new Timeframe(now, now.plusDays(1));
        }
        // Datadis requires end + 1 to get the data for the last day
        return new Timeframe(now, energyDataTimeframe.end().plusDays(1));
    }
}
