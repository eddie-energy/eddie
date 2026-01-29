// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies;

import energy.eddie.api.agnostic.data.needs.Timeframe;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;

public class PermissionEndIsEnergyDataEndStrategy implements PermissionTimeframeStrategy {

    /**
     * Calculates that timeframe of the permission that is needed to request all energy data in its timeframe. For
     * example, past energy data can be requested in one day, but future energy data needs permission to request it
     * until the end of the energy data timeframe.
     *
     * @param energyDataTimeframe the energy data timeframe that is the basis of the calculation
     * @param referenceDateTime   the reference datetime used to calculate the timeframe
     * @return the start and end date of the permission
     */
    @Override
    public Timeframe permissionTimeframe(@Nullable Timeframe energyDataTimeframe, ZonedDateTime referenceDateTime) {
        var now = referenceDateTime.toLocalDate();
        if (energyDataTimeframe != null && energyDataTimeframe.end().isAfter(now)) {
            return new Timeframe(now, energyDataTimeframe.end());
        }
        return new Timeframe(now, now);
    }
}
