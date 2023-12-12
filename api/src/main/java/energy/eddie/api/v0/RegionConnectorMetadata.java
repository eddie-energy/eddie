package energy.eddie.api.v0;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

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
     * Country code of the region covered by a {@link RegionConnector}.
     * Must be uppercase.
     */
    @JsonProperty
    String countryCode();

    /**
     * Number of metering points that are accessible through a {@link RegionConnector}.
     */
    @JsonProperty
    long coveredMeteringPoints();
}