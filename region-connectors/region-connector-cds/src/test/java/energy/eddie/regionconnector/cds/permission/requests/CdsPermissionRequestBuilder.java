package energy.eddie.regionconnector.cds.permission.requests;

import energy.eddie.api.v0.PermissionProcessStatus;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

public class CdsPermissionRequestBuilder {
    private final Map<String, ZonedDateTime> lastMeterReadings = new HashMap<>();
    private String permissionId;
    private String connectionId;
    private String dataNeedId;
    private PermissionProcessStatus status;
    private long cdsServer;
    private ZonedDateTime created;
    private LocalDate dataStart;
    private LocalDate dataEnd;
    private String state;
    private String redirectUri;

    public CdsPermissionRequestBuilder setPermissionId(String permissionId) {
        this.permissionId = permissionId;
        return this;
    }

    public CdsPermissionRequestBuilder setConnectionId(String connectionId) {
        this.connectionId = connectionId;
        return this;
    }

    public CdsPermissionRequestBuilder setDataNeedId(String dataNeedId) {
        this.dataNeedId = dataNeedId;
        return this;
    }

    public CdsPermissionRequestBuilder setStatus(PermissionProcessStatus status) {
        this.status = status;
        return this;
    }

    public CdsPermissionRequestBuilder setCdsServer(long cdsServer) {
        this.cdsServer = cdsServer;
        return this;
    }

    public CdsPermissionRequestBuilder setCreated(ZonedDateTime created) {
        this.created = created;
        return this;
    }

    public CdsPermissionRequestBuilder setDataStart(LocalDate dataStart) {
        this.dataStart = dataStart;
        return this;
    }

    public CdsPermissionRequestBuilder setDataEnd(LocalDate dataEnd) {
        this.dataEnd = dataEnd;
        return this;
    }

    public CdsPermissionRequestBuilder setState(String state) {
        this.state = state;
        return this;
    }

    public CdsPermissionRequestBuilder setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }

    public CdsPermissionRequestBuilder addLastMeterReading(String meter, ZonedDateTime reading) {
        lastMeterReadings.put(meter, reading);
        return this;
    }

    public CdsPermissionRequest build() {
        return new CdsPermissionRequest(permissionId,
                                        connectionId,
                                        dataNeedId,
                                        status,
                                        cdsServer,
                                        created,
                                        dataStart,
                                        dataEnd,
                                        state,
                                        redirectUri,
                                        lastMeterReadings);
    }
}