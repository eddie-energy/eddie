package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

public record GetMandateResponseModelApiDataResponse(
        @JsonProperty("metaData") @Nullable ApiMetaData metaData,
        @JsonProperty("data") @Nullable GetMandateResponseModel data
) {}