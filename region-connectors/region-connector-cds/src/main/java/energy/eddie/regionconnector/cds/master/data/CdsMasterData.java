package energy.eddie.regionconnector.cds.master.data;

import energy.eddie.api.agnostic.master.data.MasterData;
import energy.eddie.api.agnostic.master.data.MeteredDataAdministrator;
import energy.eddie.api.agnostic.master.data.PermissionAdministrator;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static energy.eddie.regionconnector.cds.CdsRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Component
public class CdsMasterData implements MasterData {
    private final CdsServerRepository repository;

    public CdsMasterData(CdsServerRepository repository) {this.repository = repository;}

    @Override
    public List<PermissionAdministrator> permissionAdministrators() {
        return repository.findAll()
                .stream()
                .map(CdsMasterData::toPermissionAdministrator)
                .toList();
    }

    @Override
    public Optional<PermissionAdministrator> getPermissionAdministrator(String id) {
        return repository.findById(Long.parseLong(id))
                .map(CdsMasterData::toPermissionAdministrator);
    }

    @Override
    public List<MeteredDataAdministrator> meteredDataAdministrators() {
        return repository.findAll()
                .stream()
                .map(CdsMasterData::toMeteredDataAdministrator)
                .toList();
    }

    @Override
    public Optional<MeteredDataAdministrator> getMeteredDataAdministrator(String id) {
        return repository.findById(Long.parseLong(id))
                .map(CdsMasterData::toMeteredDataAdministrator);
    }

    private static PermissionAdministrator toPermissionAdministrator(CdsServer server) {
        return new PermissionAdministrator("US",
                                           server.name(),
                                           server.displayName(),
                                           server.id(),
                                           server.baseUri(),
                                           REGION_CONNECTOR_ID);
    }

    private static MeteredDataAdministrator toMeteredDataAdministrator(CdsServer server) {
        var website = server.baseUri();
        return new MeteredDataAdministrator("US",
                                            server.name(),
                                            server.id(),
                                            website,
                                            website,
                                            server.id());
    }
}
