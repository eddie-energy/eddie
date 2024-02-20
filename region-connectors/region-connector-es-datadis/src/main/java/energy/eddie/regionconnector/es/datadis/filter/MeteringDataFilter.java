package energy.eddie.regionconnector.es.datadis.filter;

import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Filter metering data by permission request start and end date
 *
 * @param meteringData      list of metering data
 * @param permissionRequest permission request
 */
public record MeteringDataFilter(List<MeteringData> meteringData, EsPermissionRequest permissionRequest) {
    public Mono<List<MeteringData>> filter() {
        ZonedDateTime startDate = permissionRequest.start();
        Optional<ZonedDateTime> endDate = Optional.ofNullable(permissionRequest.end());
        var filteredMeteringData = meteringData.stream()
                .filter(meteringData -> inRequestedRange(meteringData, startDate, endDate))
                .toList();

        return filteredMeteringData.isEmpty() ? Mono.empty() : Mono.just(filteredMeteringData);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private boolean inRequestedRange(MeteringData meteringData, ZonedDateTime startDate, Optional<ZonedDateTime> endDate) {
        return isAfterStart(meteringData, startDate) && endDate.map(localDate -> isEqualOrBeforeEnd(meteringData, localDate)).orElse(true);
    }

    /**
     * Check if the datetime of the metering data is after the start date of the permission request
     * The start of the permission request is always at 00:00 of the day.
     * This check makes sure that the first included metering data is at 01:00 which represents the first hour of the day
     * The first 00:00 of the day is not included because it is the last metering data of the previous day
     */
    private boolean isAfterStart(MeteringData meteringData, ZonedDateTime startDate) {
        return meteringData.dateTime().isAfter(startDate);
    }

    /**
     * Check if the datetime of the metering data is equal or before the end date of the permission request
     * The end of the permission request is always at 00:00 of the end day.
     * This check makes sure that the last included metering data is at 00:00 which represents the last hour of the previous day
     */
    private boolean isEqualOrBeforeEnd(MeteringData meteringData, ZonedDateTime endDate) {
        return !meteringData.dateTime().isAfter(endDate);
    }
}