package energy.eddie.regionconnector.es.datadis.permission.request.api;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.AllowedGranularity;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import jakarta.annotation.Nullable;

import java.util.Optional;

public interface EsPermissionRequest extends MeterReadingPermissionRequest {
    /**
     * The NIF of the customer that requested the permission. This is the username which is used to log in to the
     * Datadis portal.
     */
    String nif();

    /**
     * The metering point id of the metering point associated with this permission request. In DataDis this is called
     * CUPS.
     */
    String meteringPointId();

    /**
     * The distributor code of the distributor that is responsible for the metering point associated with this
     * permission request.
     */
    Optional<DistributorCode> distributorCode();

    /**
     * This number describes what kind of metering point is associated with this permission request. The values range
     * from 1 to 5 and decide what MeasurementType is used supported.
     * <p>Only 1 and 2 support @{@link MeasurementType#QUARTER_HOURLY}.</p>
     * <p>All metering points support @{@link MeasurementType#HOURLY}.</p>
     */
    Optional<Integer> pointType();

    /**
     * In Spain, a metering point always has consumption, but they can additionally have production as well.
     *
     * @return true if the metering point associated with this permission request supports production data.
     */
    boolean productionSupport();

    /**
     * Decide what kind of metering data is requested.
     * <p>Either @{@link MeasurementType#HOURLY} or @{@link MeasurementType#QUARTER_HOURLY}.</p>
     * <p>Look at@{@link #pointType()} to see what is supported.</p>
     */
    MeasurementType measurementType();

    AllowedGranularity allowedGranularity();

    @Nullable
    String errorMessage();

    Granularity granularity();
}
