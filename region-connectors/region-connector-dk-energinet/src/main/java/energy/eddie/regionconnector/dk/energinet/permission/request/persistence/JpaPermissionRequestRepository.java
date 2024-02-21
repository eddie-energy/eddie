package energy.eddie.regionconnector.dk.energinet.permission.request.persistence;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetCustomerPermissionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Should only be used by {@link DkEnerginetCustomerPermissionRequestRepository}. Other classes should use {@link DkEnerginetCustomerPermissionRequestRepository}.
 */
@Repository
public interface JpaPermissionRequestRepository extends JpaRepository<EnerginetCustomerPermissionRequest, String> {
    List<EnerginetCustomerPermissionRequest> findAllByStatusIs(PermissionProcessStatus status);
}
