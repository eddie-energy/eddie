package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.shared.permission.requests.PermissionRequestProxy;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PermissionRequestFactory {
    private final EdaAdapter edaAdapter;
    private final Set<Extension<AtPermissionRequest>> extensions;
    private final AtConfiguration atConfiguration;

    public PermissionRequestFactory(EdaAdapter edaAdapter, Set<Extension<AtPermissionRequest>> extensions, AtConfiguration atConfiguration) {
        this.edaAdapter = edaAdapter;
        this.extensions = extensions;
        this.atConfiguration = atConfiguration;
    }

    public AtPermissionRequest create(String connectionId, String dataNeedId, CCMORequest ccmoRequest) {
        return PermissionRequestProxy.createProxy(
                new EdaPermissionRequest(connectionId, dataNeedId, ccmoRequest, edaAdapter, atConfiguration),
                extensions, AtPermissionRequest.class,
                PermissionRequestProxy.CreationInfo.NEWLY_CREATED);

    }

    public AtPermissionRequest create(AtPermissionRequest permissionRequest) {
        return PermissionRequestProxy.createProxy(
                permissionRequest,
                extensions,
                AtPermissionRequest.class,
                PermissionRequestProxy.CreationInfo.RECREATED
        );
    }
}
