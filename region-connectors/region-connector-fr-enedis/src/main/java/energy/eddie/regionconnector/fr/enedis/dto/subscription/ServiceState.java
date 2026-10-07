// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ServiceState {
    @JsonProperty("DEMANDE")
    REQUESTED,
    @JsonProperty("ACTIF")
    ACTIVE,
    @JsonProperty("TERMINE")
    TERMINATED,
    @JsonProperty("SUPPRIME")
    REMOVED
}
