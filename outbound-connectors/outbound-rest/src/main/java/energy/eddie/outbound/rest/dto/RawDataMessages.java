package energy.eddie.outbound.rest.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import energy.eddie.api.agnostic.RawDataMessage;
import jakarta.annotation.Nullable;

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
