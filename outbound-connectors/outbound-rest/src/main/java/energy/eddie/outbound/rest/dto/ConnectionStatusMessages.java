package energy.eddie.outbound.rest.dto;

import energy.eddie.api.agnostic.ConnectionStatusMessage;
import jakarta.annotation.Nullable;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

public class ConnectionStatusMessages {
    @Nullable
    private final List<ConnectionStatusMessage> documents;

    public ConnectionStatusMessages(@Nullable List<ConnectionStatusMessage> documents) {this.documents = documents;}

    // Required by jackson
    @SuppressWarnings("unused")
    @Nullable
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "ConnectionStatusMessage")
    public List<ConnectionStatusMessage> getMessages() {
        return documents;
    }
}
