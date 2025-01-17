package energy.eddie.api.v0;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedInterface;
import energy.eddie.api.agnostic.data.needs.EnergyType;

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

    /**
     * List of supported granularities.
     */
    @JsonProperty
    List<Granularity> supportedGranularities();

    @JsonProperty
    ZoneId timeZone();

    /**
     * The energy types for which the region connector can request validated historical data.
     * @return the supported energy types
     */
    @JsonProperty
    List<EnergyType> supportedEnergyTypes();

    /**
     * Returns the supported granularities for one energy type.
     * @param energyType the energy type
     * @return the granularities in which the validated historical data is available for that energy type
     */
    default List<Granularity> granularitiesFor(EnergyType energyType) {
        return supportedGranularities();
    }

    /**
     * List of supported Data Needs
     */
    @JsonIgnore
    List<Class<? extends DataNeedInterface>> supportedDataNeeds();
}