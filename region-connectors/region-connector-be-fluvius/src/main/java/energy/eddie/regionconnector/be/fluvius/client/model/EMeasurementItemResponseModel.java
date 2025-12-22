package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Optional;

public final class EMeasurementItemResponseModel implements InjectionAndOfftakeMeasurementResponseModel {
    @JsonProperty("unit")
    @Nullable
    private final String unit;
    @JsonProperty("offtakeDayValue")
    @Nullable
    private final Double offtakeDayValue;
    @JsonProperty("offtakeDayValidationState")
    private final ValidationState offtakeDayValidationState;
    @JsonProperty("offtakeNightValue")
    @Nullable
    private final Double offtakeNightValue;
    @JsonProperty("offtakeNightValidationState")
    private final ValidationState offtakeNightValidationState;
    @JsonProperty("injectionDayValue")
    @Nullable
    private final Double injectionDayValue;
    @JsonProperty("injectionDayValidationState")
    private final ValidationState injectionDayValidationState;
    @JsonProperty("injectionNightValue")
    @Nullable
    private final Double injectionNightValue;
    @JsonProperty("injectionNightValidationState")
    private final ValidationState injectionNightValidationState;

    @JsonCreator
    public EMeasurementItemResponseModel(
            @JsonProperty("unit") @Nullable String unit,
            @JsonProperty("offtakeDayValue") @Nullable Double offtakeDayValue,
            @JsonProperty("offtakeDayValidationState") ValidationState offtakeDayValidationState,
            @JsonProperty("offtakeNightValue") @Nullable Double offtakeNightValue,
            @JsonProperty("offtakeNightValidationState") ValidationState offtakeNightValidationState,
            @JsonProperty("injectionDayValue") @Nullable Double injectionDayValue,
            @JsonProperty("injectionDayValidationState") ValidationState injectionDayValidationState,
            @JsonProperty("injectionNightValue") @Nullable Double injectionNightValue,
            @JsonProperty("injectionNightValidationState") ValidationState injectionNightValidationState
    ) {
        this.unit = unit;
        this.offtakeDayValue = offtakeDayValue;
        this.offtakeDayValidationState = offtakeDayValidationState;
        this.offtakeNightValue = offtakeNightValue;
        this.offtakeNightValidationState = offtakeNightValidationState;
        this.injectionDayValue = injectionDayValue;
        this.injectionDayValidationState = injectionDayValidationState;
        this.injectionNightValue = injectionNightValue;
        this.injectionNightValidationState = injectionNightValidationState;
    }

    @Nullable
    @Override
    public String unit() {
        return unit;
    }

    @Override
    @Nonnull
    public Double offtakeValue() {
        return sumNullSafe(offtakeDayValue, offtakeNightValue);
    }

    @Override
    public boolean isOfftakePresent() {
        return offtakeDayValue != null || offtakeNightValue != null;
    }

    @Override
    public ValidationState offtakeValidationState() {
        return offtakeDayValidationState == offtakeNightValidationState ? offtakeNightValidationState : ValidationState.EST;
    }

    @Override
    @Nonnull
    public Double injectionValue() {
        return sumNullSafe(injectionDayValue, injectionNightValue);
    }

    @Override
    public ValidationState injectionValidationState() {
        return injectionDayValidationState == injectionNightValidationState ? injectionDayValidationState : ValidationState.EST;
    }

    @Override
    public boolean isInjectionPresent() {
        return injectionDayValue != null || injectionNightValue != null;
    }


    private Double sumNullSafe(@Nullable Double first, @Nullable Double second) {
        return Optional.ofNullable(first).orElse(0.0) + Optional.ofNullable(second).orElse(0.0);
    }
}