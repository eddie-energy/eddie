// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.requests;

import jakarta.annotation.Nullable;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class DsoIdAndMeteringPoint {
    public static final int DSO_ID_LENGTH = 8;


    @Nullable
    private final String meteringPoint;

    private final String dsoId;

    public DsoIdAndMeteringPoint(String dsoId, @Nullable String meteringPoint) {
        this.dsoId = requireNonNull(dsoId);
        this.meteringPoint = meteringPoint;
    }

    public String dsoId() {
        return dsoId;
    }

    public Optional<String> meteringPoint() {
        return Optional.ofNullable(meteringPoint);
    }
}
