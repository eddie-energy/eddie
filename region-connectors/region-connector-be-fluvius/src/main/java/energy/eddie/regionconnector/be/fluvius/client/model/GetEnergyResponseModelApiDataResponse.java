package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

public record GetEnergyResponseModelApiDataResponse(
        @JsonProperty("metaData") @Nullable ApiMetaData metaData,
        @JsonProperty("data") @Nullable GetEnergyResponseModel data
) {
    public GetEnergyResponseModelApiDataResponse() {
        this(null, null);
    }
}