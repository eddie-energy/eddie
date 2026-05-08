// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.client.model.v3.energy;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum EnergyType {
    @JsonProperty("E")
    ELECTRICITY,
    @JsonProperty("G")
    GAS
}
