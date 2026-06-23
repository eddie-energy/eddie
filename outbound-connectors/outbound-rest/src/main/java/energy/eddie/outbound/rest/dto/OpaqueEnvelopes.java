// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.dto;

import energy.eddie.cim.agnostic.OpaqueEnvelope;
import jakarta.annotation.Nullable;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class OpaqueEnvelopes {
    @Nullable
    private final List<OpaqueEnvelope> documents;

    public OpaqueEnvelopes(@Nullable List<OpaqueEnvelope> documents) {
        this.documents = documents;
    }

    @SuppressWarnings("unused")
    public OpaqueEnvelopes() {
        this.documents = null;
    }

    // Required by jackson
    @SuppressWarnings("unused")
    @Nullable
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "OpaqueEnvelope")
    public List<OpaqueEnvelope> getMessages() {
        return documents;
    }
}
