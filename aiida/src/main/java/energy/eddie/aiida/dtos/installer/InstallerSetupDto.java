package energy.eddie.aiida.dtos.installer;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record InstallerSetupDto(
        @JsonProperty("customValues") List<String> customValues
) {}