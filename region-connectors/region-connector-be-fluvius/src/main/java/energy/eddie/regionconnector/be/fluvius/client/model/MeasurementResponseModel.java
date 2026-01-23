// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model;

import jakarta.annotation.Nullable;

public interface MeasurementResponseModel {
    @Nullable
    String unit();

    Double offtakeValue();

    default Double value() {
        return offtakeValue();
    }

    boolean isOfftakePresent();

    default boolean isInjectionPresent() {
        return false;
    }

    ValidationState offtakeValidationState();

    default ValidationState[] validationStates() {
        return new ValidationState[]{offtakeValidationState()};
    }
}
