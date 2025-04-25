package energy.eddie.aiida.adapters.datasource.fr.transformer;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MicroTeleinfoV3Timestamp(@JsonProperty String dst, @JsonProperty String date) {}
