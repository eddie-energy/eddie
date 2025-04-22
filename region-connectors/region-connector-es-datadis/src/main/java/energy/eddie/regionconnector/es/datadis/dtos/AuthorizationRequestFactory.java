package energy.eddie.regionconnector.es.datadis.dtos;

import energy.eddie.api.agnostic.data.needs.Timeframe;
import energy.eddie.regionconnector.es.datadis.data.needs.calculation.strategies.DatadisStrategy;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.shared.services.data.needs.calculation.strategies.PermissionTimeframeStrategy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;


@Component
public class AuthorizationRequestFactory {
    private final PermissionTimeframeStrategy strategy = new DatadisStrategy();
    public AuthorizationRequest fromPermissionRequest(EsPermissionRequest permissionRequest) {
        return from(permissionRequest.nif(), permissionRequest.meteringPointId(), permissionRequest.end());
    }

    public AuthorizationRequest from(String nif, String meteringPointId, LocalDate end) {
        LocalDate permissionStart = LocalDate.now(ZONE_ID_SPAIN);
        LocalDate permissionEnd = strategy.permissionTimeframe(new Timeframe(permissionStart, end),
                                                               ZonedDateTime.now(ZONE_ID_SPAIN)).end();

        return new AuthorizationRequest(
                permissionStart,
                permissionEnd,
                nif,
                List.of(meteringPointId)
        );
    }
}
