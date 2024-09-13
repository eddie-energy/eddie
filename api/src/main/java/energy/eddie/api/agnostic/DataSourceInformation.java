package energy.eddie.api.agnostic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Information about the data source such as the country code, the region connector id, the metered data administrator id and the permission administrator id.
 */
@JsonSerialize(as = DataSourceInformation.class)
@JsonPropertyOrder(alphabetic = true)
public interface DataSourceInformation {

    /**
     * The country code of the data source.
     */
    @JsonProperty
    String countryCode();

    /**
     * The region connector id of the data source.
     */
    @JsonProperty
    String regionConnectorId();

    /**
     * The metered data administrator id of the data source.
     */
    @JsonProperty
    String meteredDataAdministratorId();

    /**
     * The permission administrator id of the data source.
     */
    @JsonProperty
    String permissionAdministratorId();
}
