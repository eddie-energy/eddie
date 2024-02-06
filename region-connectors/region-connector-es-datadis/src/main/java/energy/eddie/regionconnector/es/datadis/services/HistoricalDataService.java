package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

@Service
public class HistoricalDataService {

    private final DataApiService dataApiService;

    public HistoricalDataService(DataApiService dataApiService) {
        this.dataApiService = dataApiService;
    }


    public void fetchAvailableHistoricalData(EsPermissionRequest permissionRequest) {
        LocalDate now = ZonedDateTime.now(ZoneOffset.UTC).toLocalDate();
        LocalDate start = permissionRequest.start().toLocalDate();
        // check if request data from is in the future or today
        if (start.isAfter(now) || start.isEqual(now)) {
            return;
        }

        LocalDate end = Optional.ofNullable(permissionRequest.end())
                .map(ZonedDateTime::toLocalDate)
                .filter(permissionRequestEnd -> !permissionRequestEnd.isAfter(now))
                .orElse(now.minusDays(1));

        dataApiService.fetchDataForPermissionRequest(permissionRequest, start, end);
    }
}