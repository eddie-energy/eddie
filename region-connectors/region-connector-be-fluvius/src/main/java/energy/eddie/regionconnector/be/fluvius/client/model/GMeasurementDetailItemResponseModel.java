package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

public record GMeasurementDetailItemResponseModel(
        @JsonProperty("unit") @Nullable String unit,
        @JsonProperty("offtakeValue") @Nullable Double offtakeValue,
        @JsonProperty("offtakeValidationState") @Nullable String offtakeValidationState,
        @JsonProperty("offtakeUsedGCF") @Nullable String offtakeUsedGCF
) {}