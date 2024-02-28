package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.shared.permission.requests.PermissionRequestProxy;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PermissionRequestFactory {
    private final Set<Extension<FrEnedisPermissionRequest>> extensions;

    public PermissionRequestFactory(
            Set<Extension<FrEnedisPermissionRequest>> extensions
    ) {
        this.extensions = extensions;
    }

    /**
     * Creates a new PermissionReques, using a Proxy to handle persistence and ConnectionStatusMessages when changed.
     *
     * @param permissionRequestForCreation the DTO that is used for creating the PermissionRequest
     * @return new PermissionRequest
     */
    public FrEnedisPermissionRequest create(PermissionRequestForCreation permissionRequestForCreation) {
        FrEnedisPermissionRequest permissionRequest = new EnedisPermissionRequest(
                permissionRequestForCreation.connectionId(),
                permissionRequestForCreation.dataNeedId(),
                permissionRequestForCreation.start(),
                permissionRequestForCreation.end(),
                permissionRequestForCreation.granularity()
        );
        return PermissionRequestProxy.createProxy(
                permissionRequest,
                extensions,
                FrEnedisPermissionRequest.class,
                PermissionRequestProxy.CreationInfo.NEWLY_CREATED
        );
    }

    /**
     * Recreates a PermissionRequest.
     * The PermissionRequest is wrapped with a proxy to handle persistence and ConnectionStatusMessages when changed.
     *
     * @param permissionRequest PermissionRequest to be wrapped and recreated
     * @return recreated PermissionRequest
     */
    public FrEnedisPermissionRequest create(FrEnedisPermissionRequest permissionRequest) {
        return PermissionRequestProxy.createProxy(
                permissionRequest,
                extensions,
                FrEnedisPermissionRequest.class,
                PermissionRequestProxy.CreationInfo.RECREATED
        );
    }
}