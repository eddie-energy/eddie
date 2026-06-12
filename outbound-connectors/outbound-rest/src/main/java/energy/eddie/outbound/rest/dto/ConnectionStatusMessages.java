// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.dto;

import energy.eddie.cim.agnostic.ConnectionStatusMessage;
import jakarta.annotation.Nullable;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class ConnectionStatusMessages {
    @Nullable
    private final List<ConnectionStatusMessage> documents;

    public ConnectionStatusMessages(@Nullable List<ConnectionStatusMessage> documents) {this.documents = documents;}

    @Nullable
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "ConnectionStatusMessage")
    public List<ConnectionStatusMessage> getMessages() {
        return documents;
    }
}
