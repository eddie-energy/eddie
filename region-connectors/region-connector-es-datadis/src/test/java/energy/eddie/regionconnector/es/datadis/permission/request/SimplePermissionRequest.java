package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.DataSourceInformation;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

import java.time.ZonedDateTime;
import java.util.Optional;

public class SimplePermissionRequest implements EsPermissionRequest {
    private final String permissionId;
    private final String connectionId;
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

    public SimplePermissionRequest(String permissionId, String connectionId) {
        this.permissionId = permissionId;
        this.connectionId = connectionId;
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
    public ZonedDateTime created() {
        return null;
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
    public Optional<Integer> pointType() {
        return Optional.empty();
    }

    @Override
    public void setDistributorCodeAndPointType(DistributorCode distributorCode, Integer pointType) {

    }

    @Override
    public MeasurementType measurementType() {
        return measurementType;
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
    public ZonedDateTime start() {
        return requestDataFrom;
    }

    @Override
    public ZonedDateTime end() {
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