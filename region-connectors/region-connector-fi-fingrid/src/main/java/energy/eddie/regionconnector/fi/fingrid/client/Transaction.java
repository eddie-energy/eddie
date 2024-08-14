package energy.eddie.regionconnector.fi.fingrid.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.util.List;

public record Transaction(
        @JsonProperty("ReasonCode") @Nullable String reasonCode,
        @JsonProperty("EventReasons") @Nullable EventReasons eventReasons,
        @JsonProperty("TimeSeries") @Nullable List<TimeSeries> timeSeries
) {}
