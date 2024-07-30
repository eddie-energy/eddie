package energy.eddie.regionconnector.fr.enedis.dto.identity;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NaturalPerson(
        @JsonProperty("title")
        String title,
        @JsonProperty("firstname")
        String firstName,
        @JsonProperty("lastname")
        String lastName
) {
}
