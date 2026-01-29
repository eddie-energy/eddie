// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.v0;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedInterface;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import tools.jackson.databind.annotation.JsonSerialize;

import java.time.Period;
import java.time.ZoneId;
import java.util.List;

/**
 * Metadata for a {@link RegionConnector}.
 */
@JsonSerialize(as = RegionConnectorMetadata.class)
public interface RegionConnectorMetadata {

    /**
     * A unique identifier of a {@link RegionConnector}.
     */
    @JsonProperty
    String id();

    /**
     * Country codes of the regions covered by a {@link RegionConnector}. Must be uppercase.
     * Defaults to a single country code.
     */
    @JsonProperty
    default List<String> countryCodes() {
        return List.of(countryCode());
    }

    /**
     * Country code of the region covered by a {@link RegionConnector}. Must be uppercase.
     */
    @JsonIgnore
    String countryCode();

    /**
     * Number of metering points that are accessible through a {@link RegionConnector}.
     */
    @JsonProperty
    long coveredMeteringPoints();

    /**
     * The earliest possible start for a region-connector.
     */
    @JsonProperty
    Period earliestStart();

    /**
     * The latest possible end for a region-connector.
     */
    @JsonProperty
    Period latestEnd();

    @JsonProperty
    ZoneId timeZone();
}