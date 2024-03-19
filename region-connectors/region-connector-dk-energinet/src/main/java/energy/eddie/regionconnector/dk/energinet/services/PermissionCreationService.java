package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.SendToPermissionAdministratorException;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.agnostic.process.model.validation.ValidationException;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import org.springframework.stereotype.Service;

@Service
public class PermissionCreationService {
    private final PermissionRequestFactory requestFactory;
    private final PollingService pollingService;

    public PermissionCreationService(
            PermissionRequestFactory requestFactory,
            PollingService pollingService
    ) {
        this.requestFactory = requestFactory;
        this.pollingService = pollingService;
    }

    /**
     * Creates a new {@link PermissionRequest}, validates it and sends it to the permission administrator.
     *
     * @param requestForCreation Dto that contains the necessary information for this permission request.
     * @return The created PermissionRequest
     * @throws ValidationException                    If the {@code requestForCreation} is not valid.
     * @throws SendToPermissionAdministratorException When an error occurs while sending the request to the PA.
     *                                                If {@link SendToPermissionAdministratorException#userFault()} is true, the customer provided an invalid refresh token.
     */
    public PermissionRequest createAndSendPermissionRequest(PermissionRequestForCreation requestForCreation) throws StateTransitionException, DataNeedNotFoundException, UnsupportedDataNeedException {
        DkEnerginetCustomerPermissionRequest permissionRequest = requestFactory.create(requestForCreation);
        permissionRequest.validate();
        permissionRequest.sendToPermissionAdministrator();
        // if sendToPA doesn't fail, we have a valid refreshToken and can start polling the records in the background
        permissionRequest.receivedPermissionAdministratorResponse();
        permissionRequest.accept();
        pollingService.fetchHistoricalMeterReadings(permissionRequest);
        return permissionRequest;
    }
}
