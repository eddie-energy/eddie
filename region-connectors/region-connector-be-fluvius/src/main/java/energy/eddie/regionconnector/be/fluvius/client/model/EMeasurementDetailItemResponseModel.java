package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record EMeasurementDetailItemResponseModel(
        @JsonProperty("unit") @Nullable String unit,
        @JsonProperty("offtakeValue") @Nullable Double offtakeValue,
        @JsonProperty("offtakeValidationState") ValidationState offtakeValidationState,
        @JsonProperty("injectionValue") @Nullable Double injectionValue,
        @JsonProperty("injectionValidationState") ValidationState injectionValidationState
) implements InjectionAndOfftakeMeasurementResponseModel {
    @Override
    @Nonnull
    public Double offtakeValue() {
        return isOfftakePresent() ? offtakeValue : 0.0;
    }

    @Override
    public boolean isOfftakePresent() {
        return offtakeValue != null;
    }

    @Override
    @Nonnull
    public Double injectionValue() {
        return isInjectionPresent() ? injectionValue : 0.0;
    }

    @Override
    public boolean isInjectionPresent() {
        return injectionValue != null;
    }
}