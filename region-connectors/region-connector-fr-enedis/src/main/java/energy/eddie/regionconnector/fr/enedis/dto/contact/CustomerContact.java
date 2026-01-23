// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.contact;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CustomerContact(
        @JsonProperty("customer_id")
        String customerId,
        @JsonProperty("contact_data")
        Contact contact
) {
}
