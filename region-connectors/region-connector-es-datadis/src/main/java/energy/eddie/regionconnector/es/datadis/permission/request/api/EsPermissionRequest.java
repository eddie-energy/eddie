package energy.eddie.regionconnector.es.datadis.permission.request.api;

import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.shared.permission.requests.annotations.InvokeExtensions;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface EsPermissionRequest extends PermissionRequest {
    /**
     * The NIF of the customer that requested the permission.
     * This is the username which is used to log in to the Datadis portal.
     */
    String nif();

    /**
     * The metering point id of the metering point associated with this permission request.
     * In DataDis this is called CUPS.
     */
    String meteringPointId();

    /**
     * The distributor code of the distributor that is responsible for the metering point associated with this permission request.
     */
    Optional<DistributorCode> distributorCode();

    /**
     * This number describes what kind of metering point is associated with this permission request.
     * The values range from 1 to 5 and decide what MeasurementType is used supported.
     * Only 1 and 2 support @{@link MeasurementType#QUARTER_HOURLY}.
     * All metering points support @{@link MeasurementType#HOURLY}.
     */
    Optional<Integer> pointType();

    @InvokeExtensions
    void setDistributorCodeAndPointType(DistributorCode distributorCode, Integer pointType);

    /**
     * Decide what kind of metering data is requested.
     * Either @{@link MeasurementType#HOURLY} or @{@link MeasurementType#QUARTER_HOURLY}.
     * Look at @{@link #pointType()} to see what is supported.
     */
    MeasurementType measurementType();

    /**
     * The date the permission starts.
     * This can be different from @{@link #requestDataFrom()}, as the timeframe for which data is requested can be
     * different from the timeframe for which the permission is valid.
     */
    ZonedDateTime permissionStart();

    /**
     * The date the permission ends.
     */
    ZonedDateTime permissionEnd();

    /**
     * The date from which data is requested.
     */
    ZonedDateTime requestDataFrom();

    /**
     * The date to which data is requested.
     */
    ZonedDateTime requestDataTo();

    /**
     * The latest meter reading that was pulled for this permission request.
     * Use this to avoid pulling the same meter reading twice.
     */
    Optional<ZonedDateTime> lastPulledMeterReading();

    void setLastPulledMeterReading(ZonedDateTime lastPulledMeterReading);
}