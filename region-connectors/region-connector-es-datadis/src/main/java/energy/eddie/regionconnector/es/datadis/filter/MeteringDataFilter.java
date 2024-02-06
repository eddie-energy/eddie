package energy.eddie.regionconnector.es.datadis.filter;

import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Filter metering data by permission request start and end date
 *
 * @param meteringData      list of metering data
 * @param permissionRequest permission request
 */
public record MeteringDataFilter(List<MeteringData> meteringData, EsPermissionRequest permissionRequest) {

    public Mono<List<MeteringData>> filter() {
        var filteredMeteringData = meteringData.stream()
                .filter(meteringData -> isEqualOrAfterStart(meteringData) && isEqualOrBeforeEnd(meteringData))
                .toList();

        return filteredMeteringData.isEmpty() ? Mono.empty() : Mono.just(filteredMeteringData);
    }

    private boolean isEqualOrAfterStart(MeteringData meteringData) {
        return !meteringData.date().isBefore(permissionRequest.start().toLocalDate());
    }

    private boolean isEqualOrBeforeEnd(MeteringData meteringData) {
        var end = permissionRequest.end();
        if (end == null) {
            return true;
        }

        return !meteringData.date().isAfter(end.toLocalDate());
    }
}