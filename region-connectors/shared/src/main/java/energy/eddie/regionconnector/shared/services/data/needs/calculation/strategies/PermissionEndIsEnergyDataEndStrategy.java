package energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies;

import energy.eddie.api.agnostic.data.needs.Timeframe;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.time.ZoneId;

public class PermissionEndIsEnergyDataEndStrategy implements PermissionTimeframeStrategy {
    private final ZoneId referenceTimezone;

    public PermissionEndIsEnergyDataEndStrategy(ZoneId referenceTimezone) {this.referenceTimezone = referenceTimezone;}

    /**
     * Calculates that timeframe of the permission that is needed to request all energy data in its timeframe. For
     * example, past energy data can be requested in one day, but future energy data needs permission to request it
     * until the end of the energy data timeframe.
     *
     * @param energyDataTimeframe the energy data timeframe that is the basis of the calculation
     * @return the start and end date of the permission
     */
    @Override
    public Timeframe permissionTimeframe(@Nullable Timeframe energyDataTimeframe) {
        var now = LocalDate.now(referenceTimezone);
        if (energyDataTimeframe != null && energyDataTimeframe.end().isAfter(now)) {
            return new Timeframe(now, energyDataTimeframe.end());
        }
        return new Timeframe(now, now);
    }
}
