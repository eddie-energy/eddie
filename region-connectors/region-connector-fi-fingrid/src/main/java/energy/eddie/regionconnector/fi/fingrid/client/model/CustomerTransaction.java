package energy.eddie.regionconnector.fi.fingrid.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.List;

public record CustomerTransaction(
        @JsonProperty("AdditionalIdentification") String additionalIdentification,
        @JsonProperty("CompanyName") String companyName,
        @JsonProperty(value = "CustomerIdentification", required = true) String customerIdentification,
        @JsonProperty(value = "CustomerIdentificationType", required = true) String customerIdentificationType,
        @JsonProperty(value = "CustomerSubtype", required = true) String customerSubtype,
        @JsonProperty(value = "CustomerType", required = true) String customerType,
        @JsonProperty("DateOfBirth") ZonedDateTime dateOfBirth,
        @JsonProperty("EmailAddress") String emailAddress,
        @JsonProperty("FamilyName") String familyName,
        @JsonProperty("GivenName") String givenName,
        @JsonProperty(value = "IsInformationRestricted", required = true) boolean isInformationRestricted,
        @JsonProperty("Language") String language,
        @JsonProperty("MiddleNames") String middleNames,
        @JsonProperty("TelephoneNumber") String telephoneNumber,
        @JsonProperty(value = "CustomerPostalAddress", required = true) CustomerPostalAddress customerPostalAddress,
        @JsonProperty(value = "Agreements", required = true) List<Agreements> agreements
) {

    public List<String> meteringPointEANs() {
        return agreements.stream()
                         .map(agreement -> agreement.meteringPoint().meteringPointEAN())
                         .toList();
    }
}
