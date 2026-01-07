package energy.eddie.regionconnector.be.fluvius.client.model;

import energy.eddie.api.agnostic.Granularity;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.function.Predicate;

public interface MeterResponseModel {

    Integer seqNumber();

    @Nullable
    String meterID();

    @Nullable
    @SuppressWarnings("java:S1452")
        // Wildcards shouldn't be returned usually, but in this case with the nested hierarchy of the Fluvius responses, it is necessary.
    List<? extends EnergyItemResponseModel<?>> getByGranularity(Granularity granularity);

    default boolean hasOfftake(Granularity granularity) {
        return hasMeasurementDirection(granularity, MeasurementResponseModel::isOfftakePresent);
    }

    default boolean hasInjection(Granularity granularity) {
        return hasMeasurementDirection(granularity, MeasurementResponseModel::isInjectionPresent);
    }

    private boolean hasMeasurementDirection(
            Granularity granularity,
            Predicate<MeasurementResponseModel> measurementPredicate
    ) {
        var response = getByGranularity(granularity);
        if (response == null) {
            return false;
        }

        for (var item : response) {
            var measurements = item.measurement();
            if (measurements == null) {
                continue;
            }
            for (MeasurementResponseModel measurement : measurements) {
                if (measurementPredicate.test(measurement)) {
                    return true;
                }
            }
        }

        return false;
    }
}
