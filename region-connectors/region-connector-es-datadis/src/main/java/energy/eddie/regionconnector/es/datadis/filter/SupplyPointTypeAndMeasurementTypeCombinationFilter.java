package energy.eddie.regionconnector.es.datadis.filter;

import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.Supply;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.InvalidPointAndMeasurementTypeCombinationException;
import reactor.core.publisher.Mono;

public record SupplyPointTypeAndMeasurementTypeCombinationFilter(Supply supply, MeasurementType measurementType) {
    /**
     * Check if the point type supports the measurement type.
     * All point types support hourly data.
     * Only point types 1 and 2 support quarter hourly data.
     */
    private static boolean pointTypeSupportsMeasurementType(Integer pointType, MeasurementType measurementType) {
        return measurementType == MeasurementType.HOURLY ||
                (pointType == 1 && measurementType == MeasurementType.QUARTER_HOURLY) ||
                (pointType == 2 && measurementType == MeasurementType.QUARTER_HOURLY);
    }

    public Mono<Supply> filter() {
        var pointType = supply.pointType();

        if (pointTypeSupportsMeasurementType(pointType, measurementType)) {
            return Mono.just(supply);
        }
        return Mono.error(new InvalidPointAndMeasurementTypeCombinationException(pointType, measurementType));
    }
}