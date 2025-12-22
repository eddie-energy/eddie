package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GetEnergyResponseModelApiDataResponse(
        @JsonProperty("metaData") ApiMetaData metaData,
        @JsonProperty("data") GetEnergyResponseModel data
) {}