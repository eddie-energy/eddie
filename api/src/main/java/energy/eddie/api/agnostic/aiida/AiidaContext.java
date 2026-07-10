// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.aiida;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AiidaContext {
    FLEXIBLE_CONNECTION_AGREEMENT("FLEXIBLE-CONNECTION-AGREEMENT");

    private final String context;

    AiidaContext(String context) {
        this.context = context;
    }

    @Override
    @JsonValue
    public String toString() {
        return context;
    }
}
