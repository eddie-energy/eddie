package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

import java.time.ZonedDateTime;
import java.util.Optional;

public class SimplePermissionRequest implements EsPermissionRequest {
    private String permissionId;
    private String connectionId;
    private ZonedDateTime requestDataFrom;
    private ZonedDateTime permissionStart;
    private ZonedDateTime requestDataTo;
    private ZonedDateTime permissionEnd;
    private PermissionRequestState state;
    private String nif;
    private String dataNeedId;
    private String meteringPointId;
    private ZonedDateTime lastPulledMeterReading;
    private MeasurementType measurementType;

    public SimplePermissionRequest() {
    }

    public SimplePermissionRequest(String permissionId, String connectionId) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
    }

    public SimplePermissionRequest(String permissionId, String connectionId, ZonedDateTime start, ZonedDateTime end) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.permissionStart = start;
        this.requestDataFrom = start;
        this.permissionEnd = end;
        this.requestDataTo = end;
    }

    public SimplePermissionRequest(String permissionId, String connectionId, ZonedDateTime start, ZonedDateTime end, PermissionRequestState state) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
        this.permissionStart = start;
        this.permissionEnd = end;
        this.state = state;
    }

    static public SimplePermissionRequest fromMeasurementType(MeasurementType measurementType) {
        var request = new SimplePermissionRequest();
        request.measurementType = measurementType;
        return request;
    }

    static public SimplePermissionRequest fromMetringPointId(String meteringPointId) {
        var request = new SimplePermissionRequest();
        request.meteringPointId = meteringPointId;
        return request;
    }

    static public SimplePermissionRequest fromNif(String nif) {
        var request = new SimplePermissionRequest();
        request.nif = nif;
        return request;
    }

    static public SimplePermissionRequest fromDataNeedId(String dataNeedId) {
        var request = new SimplePermissionRequest();
        request.dataNeedId = dataNeedId;
        return request;
    }

    public static EsPermissionRequest fromLastPulledMeterReading(ZonedDateTime start) {
        var request = new SimplePermissionRequest();
        request.lastPulledMeterReading = start;
        return request;
    }

    @Override
    public String permissionId() {
        return permissionId;
    }

    @Override
    public String connectionId() {
        return connectionId;
    }

    @Override
    public String dataNeedId() {
        return dataNeedId;
    }

    @Override
    public PermissionRequestState state() {
        return state;
    }

    @Override
    public DataSourceInformation dataSourceInformation() {
        return new DatadisDataSourceInformation(this);
    }

    @Override
    public void changeState(PermissionRequestState state) {
        this.state = state;
    }

    @Override
    public String nif() {
        return nif;
    }

    @Override
    public String meteringPointId() {
        return meteringPointId;
    }

    @Override
    public Optional<DistributorCode> distributorCode() {
        return Optional.empty();
    }

    @Override
    public void setDistributorCode(DistributorCode distributorCode) {

    }

    @Override
    public Optional<Integer> pointType() {
        return Optional.empty();
    }

    @Override
    public MeasurementType measurementType() {
        return measurementType;
    }

    @Override
    public void setPointType(Integer pointType) {

    }

    @Override
    public ZonedDateTime permissionStart() {
        return permissionStart;
    }

    @Override
    public ZonedDateTime permissionEnd() {
        return permissionEnd;
    }

    @Override
    public ZonedDateTime requestDataFrom() {
        return requestDataFrom;
    }

    @Override
    public ZonedDateTime requestDataTo() {
        return requestDataTo;
    }

    @Override
    public Optional<ZonedDateTime> lastPulledMeterReading() {
        return Optional.ofNullable(lastPulledMeterReading);
    }

    @Override
    public void setLastPulledMeterReading(ZonedDateTime lastPulledMeterReading) {
        this.lastPulledMeterReading = lastPulledMeterReading;
    }
}