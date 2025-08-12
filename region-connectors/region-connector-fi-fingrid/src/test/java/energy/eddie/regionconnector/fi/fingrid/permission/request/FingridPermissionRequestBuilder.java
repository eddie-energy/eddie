package energy.eddie.regionconnector.fi.fingrid.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;

public class FingridPermissionRequestBuilder {
    private String permissionId;
    private String connectionId;
    private String dataNeedId;
    private PermissionProcessStatus status;
    private ZonedDateTime created;
    private LocalDate start;
    private LocalDate end;
    private String customerIdentification;
    private Granularity granularity;
    private Map<String, ZonedDateTime> lastMeterReadings;

    public FingridPermissionRequestBuilder setPermissionId(String permissionId) {
        this.permissionId = permissionId;
        return this;
    }

    public FingridPermissionRequestBuilder setConnectionId(String connectionId) {
        this.connectionId = connectionId;
        return this;
    }

    public FingridPermissionRequestBuilder setDataNeedId(String dataNeedId) {
        this.dataNeedId = dataNeedId;
        return this;
    }

    public FingridPermissionRequestBuilder setStatus(PermissionProcessStatus status) {
        this.status = status;
        return this;
    }

    public FingridPermissionRequestBuilder setCreated(ZonedDateTime created) {
        this.created = created;
        return this;
    }

    public FingridPermissionRequestBuilder setStart(LocalDate start) {
        this.start = start;
        return this;
    }

    public FingridPermissionRequestBuilder setEnd(LocalDate end) {
        this.end = end;
        return this;
    }

    public FingridPermissionRequestBuilder setCustomerIdentification(String customerIdentification) {
        this.customerIdentification = customerIdentification;
        return this;
    }

    public FingridPermissionRequestBuilder setGranularity(Granularity granularity) {
        this.granularity = granularity;
        return this;
    }

    public FingridPermissionRequestBuilder setLastMeterReadings(Map<String, ZonedDateTime> lastMeterReadings) {
        this.lastMeterReadings = lastMeterReadings;
        return this;
    }

    public FingridPermissionRequest build() {
        return new FingridPermissionRequest(permissionId,
                                            connectionId,
                                            dataNeedId,
                                            status,
                                            created,
                                            start,
                                            end,
                                            customerIdentification,
                                            granularity,
                                            lastMeterReadings);
    }
}