package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.shared.permission.requests.PermissionRequestProxy;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
public class PermissionRequestFactory {
    private final Set<Extension<FrEnedisPermissionRequest>> extensions;
    private final StateBuilderFactory stateBuilderFactory;

    public PermissionRequestFactory(
            Set<Extension<FrEnedisPermissionRequest>> extensions,
            StateBuilderFactory stateBuilderFactory
    ) {
        this.extensions = extensions;
        this.stateBuilderFactory = stateBuilderFactory;
    }

    /**
     * Creates a new PermissionRequest, using a Proxy to handle persistence and ConnectionStatusMessages when changed.
     *
     * @param permissionRequestForCreation the DTO that is used for creating the PermissionRequest
     * @return new PermissionRequest
     */
    public FrEnedisPermissionRequest create(
            PermissionRequestForCreation permissionRequestForCreation,
            LocalDate start,
            LocalDate end,
            Granularity granularity
    ) {
        FrEnedisPermissionRequest permissionRequest = new EnedisPermissionRequest(
                permissionRequestForCreation.connectionId(),
                permissionRequestForCreation.dataNeedId(),
                start,
                end,
                granularity,
                stateBuilderFactory
        );
        return PermissionRequestProxy.createProxy(
                permissionRequest,
                extensions,
                FrEnedisPermissionRequest.class,
                PermissionRequestProxy.CreationInfo.NEWLY_CREATED
        );
    }

    /**
     * Recreates a PermissionRequest. The PermissionRequest is wrapped with a proxy to handle persistence and
     * ConnectionStatusMessages when changed.
     *
     * @param permissionRequest PermissionRequest to be wrapped and recreated
     * @return recreated PermissionRequest
     */
    public FrEnedisPermissionRequest create(FrEnedisPermissionRequest permissionRequest) {
        return PermissionRequestProxy.createProxy(
                permissionRequest.withStateBuilderFactory(stateBuilderFactory),
                extensions,
                FrEnedisPermissionRequest.class,
                PermissionRequestProxy.CreationInfo.RECREATED
        );
    }
}
