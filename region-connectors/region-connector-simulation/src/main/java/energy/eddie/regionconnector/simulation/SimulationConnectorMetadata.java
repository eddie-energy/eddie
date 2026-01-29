// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation;

import energy.eddie.api.v0.RegionConnectorMetadata;

import javax.annotation.Nullable;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class SimulationConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "sim";

    @Nullable
    private static SimulationConnectorMetadata instance = null;

    private SimulationConnectorMetadata() {
    }

    public static SimulationConnectorMetadata getInstance() {
        if (instance == null) {
            instance = new SimulationConnectorMetadata();
        }
        return instance;
    }

    @Override
    public String id() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public String countryCode() {
        return "DE";
    }

    @Override
    public long coveredMeteringPoints() {
        return 1;
    }

    @Override
    public Period earliestStart() {
        return Period.ofYears(-1000);
    }

    @Override
    public Period latestEnd() {
        return Period.ofYears(9999);
    }

    @Override
    public ZoneId timeZone() {
        return ZoneOffset.UTC;
    }
}