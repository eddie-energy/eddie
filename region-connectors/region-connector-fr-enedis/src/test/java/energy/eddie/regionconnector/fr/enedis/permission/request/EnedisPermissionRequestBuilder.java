package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.api.UsagePointType;

import java.time.LocalDate;

public class EnedisPermissionRequestBuilder {
    private String permissionId;
    private String connectionId;
    private String dataNeedId;
    private LocalDate start;
    private LocalDate end;
    private Granularity granularity;
    private PermissionProcessStatus status;
    private String usagePointId = null;
    private UsagePointType usagePointType = UsagePointType.CONSUMPTION;

    public EnedisPermissionRequestBuilder setPermissionId(String permissionId) {
        this.permissionId = permissionId;
        return this;
    }

    public EnedisPermissionRequestBuilder setConnectionId(String connectionId) {
        this.connectionId = connectionId;
        return this;
    }

    public EnedisPermissionRequestBuilder setDataNeedId(String dataNeedId) {
        this.dataNeedId = dataNeedId;
        return this;
    }

    public EnedisPermissionRequestBuilder setStart(LocalDate start) {
        this.start = start;
        return this;
    }

    public EnedisPermissionRequestBuilder setEnd(LocalDate end) {
        this.end = end;
        return this;
    }

    public EnedisPermissionRequestBuilder setGranularity(Granularity granularity) {
        this.granularity = granularity;
        return this;
    }

    public EnedisPermissionRequestBuilder setStatus(PermissionProcessStatus status) {
        this.status = status;
        return this;
    }

    public EnedisPermissionRequestBuilder setUsagePointId(String usagePointId) {
        this.usagePointId = usagePointId;
        return this;
    }

    public EnedisPermissionRequestBuilder setUsagePointType(UsagePointType usagePointType) {
        this.usagePointType = usagePointType;
        return this;
    }

    public EnedisPermissionRequest createEnedisPermissionRequest() {
        return new EnedisPermissionRequest(permissionId,
                                           connectionId,
                                           dataNeedId,
                                           start,
                                           end,
                                           granularity,
                                           status,
                                           usagePointId,
                                           null,
                                           null,
                                           usagePointType);
    }
}