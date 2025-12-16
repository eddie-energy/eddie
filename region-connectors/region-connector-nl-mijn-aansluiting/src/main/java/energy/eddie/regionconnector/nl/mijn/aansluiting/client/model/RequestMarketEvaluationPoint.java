package energy.eddie.regionconnector.nl.mijn.aansluiting.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RequestMarketEvaluationPoint(@JsonProperty("MRID") String mrid) {}