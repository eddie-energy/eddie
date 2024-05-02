package energy.eddie.regionconnector.aiida.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.aiida.permission.request.api.AiidaPermissionRequestInterface;

public record PermissionDetailsDto(
        @JsonProperty(value = "permission_request")
        AiidaPermissionRequestInterface request,
        @JsonProperty(value = "data_need")
        DataNeed dataNeed
) {}
