package energy.eddie.aiida.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiidaAssetDto(@JsonProperty("asset") String asset) { }