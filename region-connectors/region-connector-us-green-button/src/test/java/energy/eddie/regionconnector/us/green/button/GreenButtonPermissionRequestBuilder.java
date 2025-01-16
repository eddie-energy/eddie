package energy.eddie.regionconnector.us.green.button;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.permission.request.meter.reading.MeterReading;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@SuppressWarnings("unused")
public class GreenButtonPermissionRequestBuilder {
    private String permissionId;
    private String connectionId;
    private String dataNeedId;
    private LocalDate start;
    private LocalDate end;
    private Granularity granularity;
    private PermissionProcessStatus status;
    private ZonedDateTime created;
    private String countryCode;
    private String companyId;
    private String jumpOffUrl;
    private String scope;
    private String authUid;
    private List<MeterReading> lastMeterReadings = List.of();

    public GreenButtonPermissionRequestBuilder setPermissionId(String permissionId) {
        this.permissionId = permissionId;
        return this;
    }

    public GreenButtonPermissionRequestBuilder setConnectionId(String connectionId) {
        this.connectionId = connectionId;
        return this;
    }

    public GreenButtonPermissionRequestBuilder setDataNeedId(String dataNeedId) {
        this.dataNeedId = dataNeedId;
        return this;
    }

    public GreenButtonPermissionRequestBuilder setStart(LocalDate start) {
        this.start = start;
        return this;
    }

    public GreenButtonPermissionRequestBuilder setEnd(LocalDate end) {
        this.end = end;
        return this;
    }

    public GreenButtonPermissionRequestBuilder setGranularity(Granularity granularity) {
        this.granularity = granularity;
        return this;
    }

    public GreenButtonPermissionRequestBuilder setStatus(PermissionProcessStatus status) {
        this.status = status;
        return this;
    }

    public GreenButtonPermissionRequestBuilder setCreated(ZonedDateTime created) {
        this.created = created;
        return this;
    }

    public GreenButtonPermissionRequestBuilder setCountryCode(String countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public GreenButtonPermissionRequestBuilder setCompanyId(String companyId) {
        this.companyId = companyId;
        return this;
    }

    public GreenButtonPermissionRequestBuilder setJumpOffUrl(String jumpOffUrl) {
        this.jumpOffUrl = jumpOffUrl;
        return this;
    }

    public GreenButtonPermissionRequestBuilder setScope(String scope) {
        this.scope = scope;
        return this;
    }

    public GreenButtonPermissionRequestBuilder setAuthUid(String authUid) {
        this.authUid = authUid;
        return this;
    }

    public GreenButtonPermissionRequestBuilder setLastMeterReadings(List<MeterReading> lastMeterReadings) {
        this.lastMeterReadings = lastMeterReadings;
        return this;
    }

    public GreenButtonPermissionRequest build() {
        return new GreenButtonPermissionRequest(
                permissionId,
                connectionId,
                dataNeedId,
                start,
                end,
                granularity,
                status,
                created,
                countryCode,
                companyId,
                jumpOffUrl,
                scope,
                lastMeterReadings,
                authUid
        );
    }
}
