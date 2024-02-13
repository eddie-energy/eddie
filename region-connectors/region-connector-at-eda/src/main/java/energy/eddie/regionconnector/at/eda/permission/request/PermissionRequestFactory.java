package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.shared.permission.requests.PermissionRequestProxy;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PermissionRequestFactory {
    private final Set<Extension<AtPermissionRequest>> extensions;
    private final StateBuilderFactory factory;

    public PermissionRequestFactory(Set<Extension<AtPermissionRequest>> extensions, StateBuilderFactory factory) {
        this.extensions = extensions;
        this.factory = factory;
    }

    public AtPermissionRequest create(String connectionId, String dataNeedId, CCMORequest ccmoRequest) {
        return PermissionRequestProxy.createProxy(
                new EdaPermissionRequest(connectionId, dataNeedId, ccmoRequest, factory),
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
