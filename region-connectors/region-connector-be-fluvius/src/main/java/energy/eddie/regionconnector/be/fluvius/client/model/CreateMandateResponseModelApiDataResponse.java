package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

public record CreateMandateResponseModelApiDataResponse(
        @Nullable @JsonProperty("metaData") ApiMetaData metaData,
        @Nullable @JsonProperty("data") CreateMandateResponseModel data
) {}

