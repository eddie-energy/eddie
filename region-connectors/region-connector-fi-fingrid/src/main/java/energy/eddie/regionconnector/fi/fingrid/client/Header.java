package energy.eddie.regionconnector.fi.fingrid.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

public record Header(
        @JsonProperty("Identification") String identification,
        @JsonProperty("PhysicalSenderParty") Party physicalSenderParty,
        @JsonProperty("JuridicalSenderParty") Party juridicalSenderParty,
        @JsonProperty("PhysicalReceiverParty") Party physicalReceiverParty,
        @JsonProperty("JuridicalReceiverParty") Party juridicalReceiverParty,
        @JsonProperty("ProcessRole") String processRole,
        @JsonProperty("Creation") ZonedDateTime creation,
        @JsonProperty("DocumentType") String documentType,
        @JsonProperty("OrganisationUser") String organisationUser
) {}
