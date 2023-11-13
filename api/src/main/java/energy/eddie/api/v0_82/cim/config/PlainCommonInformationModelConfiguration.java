package energy.eddie.api.v0_82.cim.config;

import energy.eddie.cim.validated_historical_data.v0_82.CodingSchemeTypeList;

public record PlainCommonInformationModelConfiguration(
        CodingSchemeTypeList eligiblePartyNationalCodingScheme) implements CommonInformationModelConfiguration {
}