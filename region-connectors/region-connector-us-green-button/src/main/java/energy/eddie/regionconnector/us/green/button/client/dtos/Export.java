package energy.eddie.regionconnector.us.green.button.client.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URL;

@SuppressWarnings("unused")
public class Export {
    @JsonProperty(required = true)
    private final String name;
    @JsonProperty(required = true)
    private final URL link;

    @JsonCreator
    public Export(String name, URL link) {
        this.name = name;
        this.link = link;
    }
}
