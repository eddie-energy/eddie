package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

public record EMeasurementItemResponseModel(
        @JsonProperty("unit") @Nullable String unit,
        @JsonProperty("offtakeDayValue") @Nullable Double offtakeDayValue,
        @JsonProperty("offtakeDayValidationState") @Nullable String offtakeDayValidationState,
        @JsonProperty("offtakeNightValue") @Nullable Double offtakeNightValue,
        @JsonProperty("offtakeNightValidationState") @Nullable String offtakeNightValidationState,
        @JsonProperty("injectionDayValue") @Nullable Double injectionDayValue,
        @JsonProperty("injectionDayValidationState") @Nullable String injectionDayValidationState,
        @JsonProperty("injectionNightValue") @Nullable Double injectionNightValue,
        @JsonProperty("injectionNightValidationState") @Nullable String injectionNightValidationState
) {}

