package energy.eddie.regionconnector.es.datadis.filter;

import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Filter metering data by permission request start and end date
 *
 * @param intermediateMeteringData list of metering data
 * @param permissionRequest        permission request
 */
public record MeteringDataFilter(
        IntermediateMeteringData intermediateMeteringData,
        EsPermissionRequest permissionRequest
) {
    public static final int NR_OF_HOURS_IN_A_DAY = 24;
    public static final int NR_OF_QUARTER_HOURS_IN_A_DAY = 96;

    public Mono<IntermediateMeteringData> filter() {
        ZonedDateTime startDate = permissionRequest.lastPulledMeterReading().orElse(permissionRequest.start());
        Optional<ZonedDateTime> endDate = Optional.ofNullable(permissionRequest.end());
        int stepSize = stepSize();
        var startIndex = calculateStartIndex(intermediateMeteringData, startDate, stepSize);
        var endIndex = calculateEndIndex(intermediateMeteringData, endDate, stepSize);

        // If the start index is greater than the end index, there is no data to return
        if (startIndex > endIndex) {
            return Mono.empty();
        }

        var filteredMeteringData = intermediateMeteringData.meteringData().subList(startIndex, endIndex);

        if (filteredMeteringData.isEmpty()) {
            return Mono.empty();
        }

        return Mono.just(IntermediateMeteringData.fromMeteringData(filteredMeteringData));
    }

    private int stepSize() {
        return permissionRequest.measurementType() == MeasurementType.HOURLY ? NR_OF_HOURS_IN_A_DAY : NR_OF_QUARTER_HOURS_IN_A_DAY;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private int calculateEndIndex(IntermediateMeteringData intermediateMeteringData, Optional<ZonedDateTime> endDate, int stepSize) {
        if (endDate.isEmpty() || !intermediateMeteringData.end().isAfter(endDate.get())) {
            return intermediateMeteringData.meteringData().size();
        }
        int daysBetween = (int) ChronoUnit.DAYS.between(endDate.get().toLocalDate(), intermediateMeteringData.end());
        return intermediateMeteringData.meteringData().size() - (daysBetween * stepSize);
    }

    private int calculateStartIndex(IntermediateMeteringData intermediateMeteringData, ZonedDateTime startDate, int stepSize) {
        if (startDate.isBefore(intermediateMeteringData.start())) {
            return 0;
        }
        int daysBetween = (int) ChronoUnit.DAYS.between(intermediateMeteringData.start().toLocalDate(), startDate);
        return daysBetween * stepSize;
    }
}
