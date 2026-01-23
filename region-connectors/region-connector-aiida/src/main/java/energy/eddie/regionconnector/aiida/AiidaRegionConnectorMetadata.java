// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida;

import energy.eddie.api.v0.RegionConnectorMetadata;

import javax.annotation.Nullable;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class AiidaRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "aiida";
    public static final ZoneId REGION_CONNECTOR_ZONE_ID = ZoneOffset.UTC;
    public static final String MQTT_CLIENT_ID = "eddie-region-connector-aiida";
    public static final Period EARLIEST_START = Period.ZERO;
    public static final Period LATEST_END = Period.ofYears(9999);

    @Nullable
    private static AiidaRegionConnectorMetadata instance = null;

    private AiidaRegionConnectorMetadata() {
    }

    public static AiidaRegionConnectorMetadata getInstance() {
        if (instance == null) {
            instance = new AiidaRegionConnectorMetadata();
        }
        return instance;
    }

    @Override
    public String id() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public String countryCode() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public long coveredMeteringPoints() {
        return 1;
    }

    @Override
    public Period earliestStart() {
        return EARLIEST_START;
    }

    @Override
    public Period latestEnd() {
        return LATEST_END;
    }

    @Override
    public ZoneId timeZone() {
        return REGION_CONNECTOR_ZONE_ID;
    }
}
