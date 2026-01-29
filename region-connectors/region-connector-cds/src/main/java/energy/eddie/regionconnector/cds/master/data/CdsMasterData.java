// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.master.data;

import energy.eddie.api.agnostic.master.data.MasterData;
import energy.eddie.api.agnostic.master.data.MeteredDataAdministrator;
import energy.eddie.api.agnostic.master.data.PermissionAdministrator;
import energy.eddie.regionconnector.cds.client.CdsServerClient;
import energy.eddie.regionconnector.cds.client.CdsServerClientFactory;
import energy.eddie.regionconnector.cds.dtos.CdsServerMasterData;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import static energy.eddie.regionconnector.cds.CdsRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Component
public class CdsMasterData implements MasterData {
    private final CdsServerClientFactory factory;

    public CdsMasterData(CdsServerClientFactory factory) {
        this.factory = factory;
    }

    @Override
    public List<PermissionAdministrator> permissionAdministrators() {
        return getAll(CdsMasterData::toPermissionAdministrators);
    }

    @Override
    public Optional<PermissionAdministrator> getPermissionAdministrator(String id) {
        return getById(id, CdsMasterData::toPermissionAdministrators);
    }

    @Override
    public List<MeteredDataAdministrator> meteredDataAdministrators() {
        return getAll(CdsMasterData::toMeteredDataAdministrator);
    }

    @Override
    public Optional<MeteredDataAdministrator> getMeteredDataAdministrator(String id) {
        return getById(id, CdsMasterData::toMeteredDataAdministrator);
    }

    private <T> List<T> getAll(Function<CdsServerMasterData, Flux<T>> mappingFunction) {
        return factory.getAll()
                      .flatMap(CdsServerClient::masterData)
                      .flatMap(mappingFunction)
                      .collectList()
                      .block();
    }

    private <T> Optional<T> getById(String id, Function<CdsServerMasterData, Flux<T>> mappingFunction) {
        var cdsServerClient = factory.get(Long.parseLong(id));
        return cdsServerClient.map(serverClient -> serverClient
                .masterData()
                .flatMapMany(mappingFunction)
                .blockFirst());
    }

    private static Flux<PermissionAdministrator> toPermissionAdministrators(CdsServerMasterData server) {
        return Flux.fromIterable(server.countries())
                   .map(countryCode -> new PermissionAdministrator(countryCode.toLowerCase(Locale.ROOT),
                                                                   server.name(),
                                                                   server.name(),
                                                                   server.id(),
                                                                   server.baseUri().toString(),
                                                                   REGION_CONNECTOR_ID)
                   );
    }

    private static Flux<MeteredDataAdministrator> toMeteredDataAdministrator(CdsServerMasterData server) {
        var website = server.baseUri().toString();
        return Flux.fromIterable(server.countries())
                   .map(countryCode ->
                                new MeteredDataAdministrator(countryCode.toLowerCase(Locale.ROOT),
                                                             server.name(),
                                                             server.id(),
                                                             website,
                                                             website,
                                                             server.id())
                   );
    }
}
