// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.cim.config;

import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;

public record PlainCommonInformationModelConfiguration(
        CodingSchemeTypeList eligiblePartyNationalCodingScheme,
        String eligiblePartyFallbackId
) implements CommonInformationModelConfiguration {
}
