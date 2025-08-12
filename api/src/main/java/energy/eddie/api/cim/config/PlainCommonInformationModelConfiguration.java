package energy.eddie.api.cim.config;

import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;

public record PlainCommonInformationModelConfiguration(
        CodingSchemeTypeList eligiblePartyNationalCodingScheme,
        String eligiblePartyFallbackId
) implements CommonInformationModelConfiguration {
}
