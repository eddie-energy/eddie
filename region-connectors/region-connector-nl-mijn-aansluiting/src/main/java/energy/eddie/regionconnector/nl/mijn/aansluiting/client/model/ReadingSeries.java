package energy.eddie.regionconnector.nl.mijn.aansluiting.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReadingSeries(@JsonProperty("marketEvaluationPoint") MarketEvaluationPoint marketEvaluationPoint) {
}

