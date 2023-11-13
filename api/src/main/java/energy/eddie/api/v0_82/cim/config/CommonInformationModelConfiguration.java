package energy.eddie.api.v0_82.cim.config;

import energy.eddie.cim.validated_historical_data.v0_82.CodingSchemeTypeList;

/**
 * Configuration for the CIM.
 * This is used to provide needed configuration values for creating CIM documents.
 */
public interface CommonInformationModelConfiguration {

    String CIM = "cim";
    String ELIGIBLE_PARTY = CIM + ".eligible-party";
    String ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY = ELIGIBLE_PARTY + ".national-coding-scheme";

    /**
     * The coding scheme identifies which country the eligible party is located in.
     *
     * @return The coding scheme for a specific country (e.g. "NAT" for Austria)
     */
    CodingSchemeTypeList eligiblePartyNationalCodingScheme();
}