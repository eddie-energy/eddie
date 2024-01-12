package energy.eddie.api.v0_82.cim.config;

import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;

public record PlainCommonInformationModelConfiguration(
        CodingSchemeTypeList eligiblePartyNationalCodingScheme) implements CommonInformationModelConfiguration {
}