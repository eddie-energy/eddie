package energy.eddie.regionconnector.cds.master.data;

import energy.eddie.api.agnostic.master.data.MasterData;
import energy.eddie.api.agnostic.master.data.MeteredDataAdministrator;
import energy.eddie.api.agnostic.master.data.PermissionAdministrator;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.cds.CdsRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Component
public class CdsMasterData implements MasterData {
    private final CdsServerRepository repository;

    public CdsMasterData(CdsServerRepository repository) {this.repository = repository;}

    @Override
    public List<PermissionAdministrator> permissionAdministrators() {
        return repository.findAll()
                         .stream()
                         .flatMap(CdsMasterData::toPermissionAdministrators)
                         .toList();
    }

    @Override
    public Optional<PermissionAdministrator> getPermissionAdministrator(String id) {
        return repository.findById(Long.parseLong(id))
                         .map(CdsMasterData::toPermissionAdministrators)
                         .flatMap(Stream::findFirst);
    }

    @Override
    public List<MeteredDataAdministrator> meteredDataAdministrators() {
        return repository.findAll()
                         .stream()
                         .flatMap(CdsMasterData::toMeteredDataAdministrator)
                         .toList();
    }

    @Override
    public Optional<MeteredDataAdministrator> getMeteredDataAdministrator(String id) {
        return repository.findById(Long.parseLong(id))
                         .map(CdsMasterData::toMeteredDataAdministrator)
                         .flatMap(Stream::findFirst);
    }

    private static Stream<PermissionAdministrator> toPermissionAdministrators(CdsServer server) {
        return server.countryCodes()
                .stream()
                .map(countryCode ->
                             new PermissionAdministrator(countryCode.toLowerCase(Locale.ROOT),
                                                         server.name(),
                                                         server.displayName(),
                                                         server.idAsString(),
                                                         server.baseUri(),
                                                         REGION_CONNECTOR_ID)
                );
    }

    private static Stream<MeteredDataAdministrator> toMeteredDataAdministrator(CdsServer server) {
        var website = server.baseUri();
        return server.countryCodes()
                .stream()
                .map(countryCode ->
                             new MeteredDataAdministrator(countryCode.toLowerCase(Locale.ROOT),
                                                          server.name(),
                                                          server.idAsString(),
                                                          website,
                                                          website,
                                                          server.idAsString())
                );
    }
}
