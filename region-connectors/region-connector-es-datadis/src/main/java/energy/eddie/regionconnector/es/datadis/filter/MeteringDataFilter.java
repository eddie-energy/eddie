package energy.eddie.regionconnector.es.datadis.filter;

import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Filter metering data by permission request start and end date to ensure only the requested data is returned.
 *
 * @param intermediateMeteringData list of metering data
 * @param permissionRequest        permission request
 */
public record MeteringDataFilter(
        IntermediateMeteringData intermediateMeteringData,
        EsPermissionRequest permissionRequest
) {

    /**
     * Filters the data using the end date of the permission request and
     * either the {@link energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest#latestMeterReadingEndDate()} or {@link EsPermissionRequest#start()}
     *
     * @return data between the {@link energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest#latestMeterReadingEndDate()} / {@link EsPermissionRequest#start()} and {@link EsPermissionRequest#end()}
     */
    public Mono<IntermediateMeteringData> filter() {
        LocalDate startDate = permissionRequest.latestMeterReadingEndDate().orElse(permissionRequest.start());
        LocalDate endDate = permissionRequest.end()
                                             .plusDays(1); // Add one day to the end date to treat end as inclusive

        return filter(startDate, endDate);
    }

    /**
     * Filters the data using the provided dates
     *
     * @param startDate data before this date is omitted
     * @param endDate   data after this date is omitted
     * @return data between the start and end date (startDate &lt; data &lt;= endDate)
     */
    public Mono<IntermediateMeteringData> filter(
            LocalDate startDate,
            LocalDate endDate
    ) {
        var iterator = intermediateMeteringData.meteringData().listIterator();
        while (iterator.hasNext()) {
            var meteringData = iterator.next();
            if (meteringData.date().isEqual(startDate)) {
                iterator.previous();
                break;
            }
        }
        var startIndex = iterator.nextIndex();
        while (iterator.hasNext()) {
            var meteringData = iterator.next();
            if (meteringData.date().isEqual(endDate)) {
                iterator.previous();
                break;
            }
        }
        var endIndex = iterator.nextIndex();

        var filteredMeteringData = intermediateMeteringData.meteringData().subList(startIndex, endIndex);

        if (filteredMeteringData.isEmpty()) {
            return Mono.empty();
        }

        return IntermediateMeteringData.fromMeteringData(filteredMeteringData);
    }
}
