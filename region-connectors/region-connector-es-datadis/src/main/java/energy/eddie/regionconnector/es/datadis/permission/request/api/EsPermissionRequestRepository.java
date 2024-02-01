package energy.eddie.regionconnector.es.datadis.permission.request.api;

import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;

import java.util.stream.Stream;

public interface EsPermissionRequestRepository extends PermissionRequestRepository<EsPermissionRequest> {

    Stream<EsPermissionRequest> findAllAccepted();
}