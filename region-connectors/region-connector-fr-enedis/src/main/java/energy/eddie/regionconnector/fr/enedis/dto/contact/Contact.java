// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.contact;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Contact(
        @JsonProperty("phone")
        String phone,
        @JsonProperty("email")
        String email
) {
}
