// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

public record Agreements(
        @JsonProperty("AgreementEndDate") ZonedDateTime agreementEndDate,
        @JsonProperty(value = "AgreementStartDate", required = true) ZonedDateTime agreementStartDate,
        @JsonProperty(value = "AgreementStatus", required = true) String agreementStatus,
        @JsonProperty(value = "AgreementType", required = true) String agreementType,
        @JsonProperty(value = "MeteringPoint", required = true) MeteringPoint meteringPoint
) {
}
