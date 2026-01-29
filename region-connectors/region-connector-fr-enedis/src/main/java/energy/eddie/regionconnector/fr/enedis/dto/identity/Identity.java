// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.dto.identity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public record Identity(
        @JsonProperty("natural_person")
        Optional<NaturalPerson> naturalPerson,
        @JsonProperty("legal_entity")
        Optional<LegalEntity> legalEntity
) {
}
