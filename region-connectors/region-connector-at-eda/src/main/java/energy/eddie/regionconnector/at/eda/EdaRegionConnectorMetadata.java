// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda;

import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;

import javax.annotation.Nullable;
import java.time.Period;
import java.time.ZoneId;

public class EdaRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "at-eda";
    public static final AllowedTransmissionCycle TRANSMISSION_CYCLE = AllowedTransmissionCycle.D;
    public static final Period PERIOD_EARLIEST_START = Period.ofMonths(-36);
    public static final Period PERIOD_LATEST_END = Period.ofMonths(36);
    public static final ZoneId AT_ZONE_ID = ZoneId.of("Europe/Vienna");
    @Nullable
    private static EdaRegionConnectorMetadata instance = null;

    private EdaRegionConnectorMetadata() {
    }

    public static EdaRegionConnectorMetadata getInstance() {
        if (instance == null) {
            instance = new EdaRegionConnectorMetadata();
        }
        return instance;
    }

    @Override
    public String id() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public String countryCode() {
        return "AT";
    }

    @Override
    public long coveredMeteringPoints() {
        return 5977915;
    }

    @Override
    public Period earliestStart() {
        return PERIOD_EARLIEST_START;
    }

    @Override
    public Period latestEnd() {
        return PERIOD_LATEST_END;
    }

    @Override
    public ZoneId timeZone() {
        return AT_ZONE_ID;
    }
}
