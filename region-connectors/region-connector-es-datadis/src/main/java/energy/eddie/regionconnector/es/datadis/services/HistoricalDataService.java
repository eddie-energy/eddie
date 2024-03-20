package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;

@Service
public class HistoricalDataService {

    private final DataApiService dataApiService;

    public HistoricalDataService(DataApiService dataApiService) {
        this.dataApiService = dataApiService;
    }


    public void fetchAvailableHistoricalData(EsPermissionRequest permissionRequest) {
        LocalDate now = LocalDate.now(ZONE_ID_SPAIN);
        LocalDate start = permissionRequest.start();
        // check if request data from is in the future or today
        if (start.isAfter(now) || start.isEqual(now)) {
            return;
        }

        LocalDate end = Optional.ofNullable(permissionRequest.end())
                .filter(permissionRequestEnd -> !permissionRequestEnd.isAfter(now))
                .orElse(now.minusDays(1));

        dataApiService.fetchDataForPermissionRequest(permissionRequest, start, end);
    }
}
