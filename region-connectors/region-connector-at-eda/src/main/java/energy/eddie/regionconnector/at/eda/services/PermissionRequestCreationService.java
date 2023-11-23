package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import energy.eddie.regionconnector.at.eda.requests.CCMOTimeFrame;
import energy.eddie.regionconnector.at.eda.requests.DsoIdAndMeteringPoint;
import energy.eddie.regionconnector.at.eda.requests.RequestDataType;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedMeteringIntervalType;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedTransmissionCycle;

public class PermissionRequestCreationService {

    private final PermissionRequestFactory permissionRequestFactory;
    private final AtConfiguration configuration;

    public PermissionRequestCreationService(
            PermissionRequestFactory permissionRequestFactory,
            AtConfiguration configuration
    ) {
        this.permissionRequestFactory = permissionRequestFactory;
        this.configuration = configuration;
    }

    public CreatedPermissionRequest createAndSendPermissionRequest(PermissionRequestForCreation permissionRequestForCreation) throws StateTransitionException {
        CCMORequest ccmoRequest = getCcmoRequest(permissionRequestForCreation);
        AtPermissionRequest permissionRequest = permissionRequestFactory.create(
                permissionRequestForCreation.connectionId(),
                permissionRequestForCreation.dataNeedId(),
                ccmoRequest
        );
        permissionRequest.validate();
        permissionRequest.sendToPermissionAdministrator();
        return new CreatedPermissionRequest(permissionRequest);
    }

    private CCMORequest getCcmoRequest(PermissionRequestForCreation permissionRequestForCreation) {
        DsoIdAndMeteringPoint dsoIdAndMeteringPoint = new DsoIdAndMeteringPoint(
                permissionRequestForCreation.dsoId(),
                permissionRequestForCreation.meteringPointId()
        );
        return new CCMORequest(
                dsoIdAndMeteringPoint,
                new CCMOTimeFrame(permissionRequestForCreation.start(), permissionRequestForCreation.end()),
                this.configuration,
                RequestDataType.METERING_DATA, // for now only allow metering data
                AllowedMeteringIntervalType.QH,
                AllowedTransmissionCycle.D
        );
    }
}
