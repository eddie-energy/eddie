package energy.eddie.regionconnector.dk.energinet.permission.request.api;

import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;

import java.util.Collection;

public interface DkEnerginetCustomerPermissionRequestRepository extends PermissionRequestRepository<DkEnerginetCustomerPermissionRequest> {
    Collection<DkEnerginetCustomerPermissionRequest> findAll();
}