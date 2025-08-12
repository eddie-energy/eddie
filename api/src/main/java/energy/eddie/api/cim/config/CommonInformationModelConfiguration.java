package energy.eddie.api.cim.config;

import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;

/**
 * Configuration for the CIM. This is used to provide needed configuration values for creating CIM documents.
 */
public interface CommonInformationModelConfiguration {

    String CIM = "cim";
    String ELIGIBLE_PARTY = CIM + ".eligible-party";
    String ELIGIBLE_PARTY_NATIONAL_CODING_SCHEME_KEY = ELIGIBLE_PARTY + ".national-coding-scheme";
    String ELIGIBLE_PARTY_FALLBACK_ID_KEY = ELIGIBLE_PARTY + ".fallback.id";

    /**
     * The coding scheme identifies which country the eligible party is located in.
     *
     * @return The coding scheme for a specific country (e.g. "NAT" for Austria)
     */
    CodingSchemeTypeList eligiblePartyNationalCodingScheme();

    /**
     * This ID is used to identify the eligible party when the region does not provide an ID for the eligible party.
     *
     * @return The fallback ID for the eligible party
     */
    String eligiblePartyFallbackId();
}
