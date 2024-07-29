package energy.eddie.regionconnector.fr.enedis.dto.contact;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Contact(
        @JsonProperty("phone")
        String phone,
        @JsonProperty("email")
        String email
) {
}
