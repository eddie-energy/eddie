package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;
import java.util.List;

public record GetMandateResponseModel(@JsonProperty("fetchTime") @Nullable ZonedDateTime fetchTime,
                                      @JsonProperty("mandates") @Nullable List<MandateResponseModel> mandates) {
}