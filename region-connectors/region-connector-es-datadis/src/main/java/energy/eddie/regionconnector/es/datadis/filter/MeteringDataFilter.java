package energy.eddie.regionconnector.es.datadis.filter;

import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

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
        LocalDate startDate = permissionRequest.lastPulledMeterReading().orElse(permissionRequest.start());
        LocalDate endDate = permissionRequest.end()
                                             .plusDays(1); // Add one day to the end date to treat end as inclusive
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

    private int calculateStartIndex(
            IntermediateMeteringData intermediateMeteringData,
            LocalDate startDate,
            int stepSize
    ) {
        if (startDate.isBefore(intermediateMeteringData.start())) {
            return 0;
        }
        int daysBetween = (int) ChronoUnit.DAYS.between(intermediateMeteringData.start(), startDate);
        return daysBetween * stepSize;
    }

    private int calculateEndIndex(IntermediateMeteringData intermediateMeteringData, LocalDate endDate, int stepSize) {
        if (!intermediateMeteringData.end().isAfter(endDate)) {
            return intermediateMeteringData.meteringData().size();
        }
        int daysBetween = (int) ChronoUnit.DAYS.between(endDate, intermediateMeteringData.end());
        return intermediateMeteringData.meteringData().size() - (daysBetween * stepSize);
    }
}
