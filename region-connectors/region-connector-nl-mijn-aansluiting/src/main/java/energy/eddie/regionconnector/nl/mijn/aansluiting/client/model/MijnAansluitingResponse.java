package energy.eddie.regionconnector.nl.mijn.aansluiting.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MijnAansluitingResponse(
        @JsonProperty("MarketEvaluationPoint")
        MarketEvaluationPoint marketEvaluationPoint
) {
}

