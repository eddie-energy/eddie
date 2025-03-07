package energy.eddie.regionconnector.cds.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.List;

public record CdsServerRedirectUriUpdate(@JsonProperty("redirect_uris") List<URI> redirectUris) {
}
