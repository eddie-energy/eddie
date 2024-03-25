package energy.eddie.regionconnector.fr.enedis.permission.request.api;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.agnostic.process.model.annotations.InvokeExtensions;
import energy.eddie.regionconnector.fr.enedis.permission.request.StateBuilderFactory;

import java.time.LocalDate;
import java.util.Optional;

public interface FrEnedisPermissionRequest extends TimeframedPermissionRequest {
    FrEnedisPermissionRequest withStateBuilderFactory(StateBuilderFactory factory);

    Optional<String> usagePointId();

    @InvokeExtensions
    void setUsagePointId(String usagePointId);

    Granularity granularity();

    /**
     * The latest meter reading that was pulled for this permission request. This is the end date of the last meter
     * reading that was pulled.
     */
    Optional<LocalDate> latestMeterReading();

    @InvokeExtensions
    void updateLatestMeterReading(LocalDate latestMeterReading);
}
