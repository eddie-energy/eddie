// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record GMeasurementItemResponseModel(
        @JsonProperty("unit") @Nullable String unit,
        @JsonProperty("offtakeValue") @Nullable Double offtakeValue,
        @JsonProperty("offtakeValidationState") ValidationState offtakeValidationState,
        @JsonProperty("offtakeUsedGCF") @Nullable String offtakeUsedGCF
) implements MeasurementResponseModel {
    @Override
    @Nonnull
    public Double offtakeValue() {
        return isOfftakePresent() ? offtakeValue : 0.0;
    }

    @Override
    public boolean isOfftakePresent() {
        return offtakeValue != null;
    }
}