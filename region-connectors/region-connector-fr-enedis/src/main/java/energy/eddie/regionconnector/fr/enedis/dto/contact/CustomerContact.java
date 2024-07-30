package energy.eddie.regionconnector.fr.enedis.dto.contact;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CustomerContact(
        @JsonProperty("customer_id")
        String customerId,
        @JsonProperty("contact_data")
        Contact contact
) {
}
