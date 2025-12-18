package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

public record FluviusSessionRequestDataServiceModel(
        @JsonProperty("dataServiceType") @Nullable String dataServiceType,
        @JsonProperty("dataPeriodFrom") @Nullable String dataPeriodFrom,
        @JsonProperty("dataPeriodTo") @Nullable String dataPeriodTo
) {}

