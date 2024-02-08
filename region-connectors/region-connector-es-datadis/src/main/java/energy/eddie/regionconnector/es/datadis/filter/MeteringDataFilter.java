package energy.eddie.regionconnector.es.datadis.filter;

import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
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
        LocalDate startDate = permissionRequest.start().toLocalDate();
        Optional<LocalDate> endDate = Optional.ofNullable(permissionRequest.end()).map(ZonedDateTime::toLocalDate);
        var filteredMeteringData = meteringData.stream()
                .filter(meteringData -> isEqualOrAfterStart(meteringData, startDate) && endDate.map(localDate -> isEqualOrBeforeEnd(meteringData, localDate)).orElse(true))
                .toList();

        return filteredMeteringData.isEmpty() ? Mono.empty() : Mono.just(filteredMeteringData);
    }

    private boolean isEqualOrAfterStart(MeteringData meteringData, LocalDate startDate) {
        return !meteringData.dateTime().toLocalDate().isBefore(startDate);
    }

    private boolean isEqualOrBeforeEnd(MeteringData meteringData, LocalDate endDate) {
        return !meteringData.dateTime().toLocalDate().isAfter(endDate);
    }
}