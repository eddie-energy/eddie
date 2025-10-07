package energy.eddie.aiida.dtos.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public record LatestOutboundPermissionRecordDto(
        @JsonProperty UUID permissionId,
        @JsonProperty String topic,
        @JsonProperty String serverUri,
        @JsonProperty List<LatestSchemaRecordDto> messages
) {
}
