package energy.eddie.aiida.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;

import java.time.LocalDate;
import java.util.Map;


@SuppressWarnings("NullAway")
public class PermissionDetailsDto {
    @JsonProperty(value = "permission_id")
    private String permissionId;
    @JsonProperty(value = "connection_id")
    private String connectionId;
    @JsonProperty(value = "start")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate start;
    @JsonProperty(value = "end")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate end;
    @JsonProperty(value = "data_need")
    private AiidaDataNeed dataNeed;

    public PermissionDetailsDto(
            String permissionId,
            String connectionId,
            LocalDate start,
            LocalDate end,
            AiidaDataNeed dataNeed
    ) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.start = start;
        this.end = end;
        this.dataNeed = dataNeed;
    }

    @JsonProperty("permission_request")
    private void unpackPermissionRequest(Map<String, String> permissionRequest) {
        permissionId = permissionRequest.get("permission_id");
        connectionId = permissionRequest.get("connection_id");
        start = LocalDate.parse(permissionRequest.get("start"));
        end = LocalDate.parse(permissionRequest.get("end"));
    }

    public String permissionId() {
        return permissionId;
    }

    public String connectionId() {
        return connectionId;
    }

    public LocalDate start() {
        return start;
    }

    public LocalDate end() {
        return end;
    }

    public AiidaDataNeed dataNeed() {
        return dataNeed;
    }
}
