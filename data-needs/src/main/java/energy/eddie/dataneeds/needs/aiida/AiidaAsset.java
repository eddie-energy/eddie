// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.needs.aiida;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum AiidaAsset {
    CONNECTION_AGREEMENT_POINT("CONNECTION-AGREEMENT-POINT"),
    CONTROLLABLE_UNIT("CONTROLLABLE-UNIT"),
    DEDICATED_MEASUREMENT_DEVICE("DEDICATED-MEASUREMENT-DEVICE"),
    SUBMETER("SUBMETER");

    private final String asset;

    AiidaAsset(String asset) {
        this.asset = asset;
    }

    @JsonCreator
    public static AiidaAsset forValue(String value) {
        return Arrays.stream(AiidaAsset.values())
                     .filter(op -> op.toString().equals(value))
                     .findFirst()
                     .orElseThrow();
    }

    @Override
    @JsonValue
    public String toString() {
        return asset;
    }
}
