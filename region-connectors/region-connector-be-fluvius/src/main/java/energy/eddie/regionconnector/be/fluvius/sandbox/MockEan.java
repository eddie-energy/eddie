package energy.eddie.regionconnector.be.fluvius.sandbox;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockEan {
    private static final Logger LOGGER = LoggerFactory.getLogger(MockEan.class);
    // EAN for quarter hourly electricity meter for consumption and production
    private static final String DEFAULT_EAN = "541440110000000001";
    // 54144 (0 -> electricity, 1 -> gas) (0 -> decrease only, 1 -> sampling & injection) (0 -> VH_day, 1 -> VH_quarter_hour) 0000000 (xxx -> 1000 meters per group)
    private static final String EAN_FORMAT = "54144%s%s%s0000000001";
    private final DataNeed dataNeed;
    private final FluviusPermissionRequest permissionRequest;
    private final Granularity granularity;

    public MockEan(DataNeed dataNeed, FluviusPermissionRequest permissionRequest, Granularity granularity) {
        this.dataNeed = dataNeed;
        this.permissionRequest = permissionRequest;
        this.granularity = granularity;
    }

    @Override
    public String toString() {
        if (!(dataNeed instanceof ValidatedHistoricalDataDataNeed vhd)) {
            return DEFAULT_EAN;
        }
        var energyType = switch (vhd.energyType()) {
            case ELECTRICITY -> "0";
            case NATURAL_GAS -> "1";
            default -> {
                LOGGER.warn("Invalid energy type {} requested by data need {}", vhd.energyType(), dataNeed.id());
                throw new IllegalArgumentException("Invalid energy type " + vhd.energyType());
            }
        };
        var granularityString = switch (granularity) {
            case P1D -> "0";
            case PT15M -> "1";
            default -> {
                LOGGER.warn("Permission request {} has invalid granularity {} for mocking data",
                            permissionRequest.permissionId(),
                            permissionRequest.granularity());
                throw new IllegalArgumentException("Invalid granularity " + permissionRequest.granularity());
            }
        };
        return EAN_FORMAT.formatted(energyType, "1", granularityString);
    }
}
