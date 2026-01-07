package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.util.List;

public record FluviusSessionRequestModel(
        @JsonProperty("dataAccessContractNumber") @Nullable String dataAccessContractNumber,
        @JsonProperty("referenceNumber") @Nullable String referenceNumber,
        @JsonProperty("flow") @Nullable String flow,
        @JsonProperty("dataServices") @Nullable List<FluviusSessionRequestDataServiceModel> dataServices,
        @JsonProperty("numberOfEans") Integer numberOfEans,
        @JsonProperty("returnUrlSuccess") @Nullable String returnUrlSuccess,
        @JsonProperty("returnUrlFailed") @Nullable String returnUrlFailed,
        @JsonProperty("sso") @Nullable Boolean sso,
        @JsonProperty("enterpriseNumber") @Nullable String enterpriseNumber
) {}
