// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.onenet;

import energy.eddie.api.v0.RegionConnectorMetadata;
import org.springframework.stereotype.Component;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class OneNetRegionConnectorMetadata implements RegionConnectorMetadata {
    public static final String REGION_CONNECTOR_ID = "onenet";

    @Override
    public String id() {
        return REGION_CONNECTOR_ID;
    }

    @Override
    public List<String> countryCodes() {
        // TODO: Update to correct countries that are supported by this region connector
        // If only one country is supported, delete this method and use the countryCode method instead.
        return List.of("IT");
    }

    @Override
    public String countryCode() {
        return countryCodes().getFirst();
    }

    @Override
    public long coveredMeteringPoints() {
        // TODO: Update to correct number of metering points
        return 0;
    }

    @Override
    public Period earliestStart() {
        // TODO: Update to correct start date
        return Period.ofYears(-3);
    }

    @Override
    public Period latestEnd() {
        // TODO: Update to correct end date
        return Period.ofYears(3);
    }

    @Override
    public ZoneId timeZone() {
        // TODO: Update to correct time zone
        return ZoneOffset.UTC;
    }
}
