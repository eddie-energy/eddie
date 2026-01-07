package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

public record CreateMandateResponseModel(@Nullable @JsonProperty("status") String status) {}

