package energy.eddie.aiida.dtos.installer;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VersionInfoDto(
        @JsonProperty("releaseName") String releaseName,
        @JsonProperty("releaseInfo") ReleaseInfoDto releaseInfo,
        @JsonProperty("installedChart") ChartMetadataDto installedChart,
        @JsonProperty("latestChart") ChartMetadataDto latestChart
) { }