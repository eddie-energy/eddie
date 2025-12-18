package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

public record CreateMandateRequestModel(
        @Nullable @JsonProperty("referenceNumber") String referenceNumber,
        @Nullable @JsonProperty("eanNumber") String eanNumber,
        @Nullable @JsonProperty("dataServiceType") String dataServiceType,
        @Nullable @JsonProperty("dataPeriodFrom") String dataPeriodFrom,
        @Nullable @JsonProperty("dataPeriodTo") String dataPeriodTo,
        @Nullable @JsonProperty("status") String status,
        @Nullable @JsonProperty("mandateExpirationDate") String mandateExpirationDate,
        @Nullable @JsonProperty("renewalStatus") String renewalStatus
) {}

