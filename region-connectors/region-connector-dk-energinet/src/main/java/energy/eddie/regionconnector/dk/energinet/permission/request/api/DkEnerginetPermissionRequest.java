package energy.eddie.regionconnector.dk.energinet.permission.request.api;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.MeterReadingPermissionRequest;

public interface DkEnerginetPermissionRequest extends MeterReadingPermissionRequest {

    String refreshToken();

    String accessToken();

    Granularity granularity();

    String meteringPoint();
}
