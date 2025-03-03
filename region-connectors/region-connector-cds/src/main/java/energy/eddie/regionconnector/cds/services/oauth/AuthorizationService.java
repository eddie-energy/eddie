package energy.eddie.regionconnector.cds.services.oauth;

import energy.eddie.regionconnector.cds.master.data.CdsServer;

import java.net.URI;

public interface AuthorizationService {
    URI createOAuthRequest(CdsServer cdsServer, String permissionId);
}
