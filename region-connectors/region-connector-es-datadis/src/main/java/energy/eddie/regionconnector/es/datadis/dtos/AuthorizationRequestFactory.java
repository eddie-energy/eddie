package energy.eddie.regionconnector.es.datadis.dtos;

import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;

import java.time.LocalDate;
import java.util.List;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;


public class AuthorizationRequestFactory {
    public AuthorizationRequest fromPermissionRequest(EsPermissionRequest permissionRequest) {
        LocalDate permissionStart = LocalDate.now(ZONE_ID_SPAIN);
        LocalDate permissionEnd = calculatePermissionEnd(permissionStart, permissionRequest.end());

        return new AuthorizationRequest(
                permissionStart,
                permissionEnd,
                permissionRequest.nif(),
                List.of(permissionRequest.meteringPointId())
        );
    }

    private LocalDate calculatePermissionEnd(LocalDate permissionStart, LocalDate permissionRequestEnd) {
        if (!permissionStart.isBefore(permissionRequestEnd)) {
            return permissionStart.plusDays(1); // if all the data is in the past we only need access for 1 day
        }

        return permissionRequestEnd.plusDays(1); // Datadis requires end + 1 in order to get the data for the last day
    }
}
