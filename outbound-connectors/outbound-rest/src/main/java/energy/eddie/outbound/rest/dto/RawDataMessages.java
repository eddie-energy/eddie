// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.dto;

import energy.eddie.api.agnostic.RawDataMessage;
import jakarta.annotation.Nullable;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class RawDataMessages {
    @Nullable
    private final List<RawDataMessage> documents;

    public RawDataMessages(@Nullable List<RawDataMessage> documents) {this.documents = documents;}

    // Required by jackson
    @SuppressWarnings("unused")
    @Nullable
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "RawDataMessage")
    public List<RawDataMessage> getMessages() {
        return documents;
    }
}
