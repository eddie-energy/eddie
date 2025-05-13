package energy.eddie.regionconnector.cds.master.data;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.agnostic.master.data.MeteredDataAdministrator;
import energy.eddie.api.agnostic.master.data.PermissionAdministrator;
import energy.eddie.regionconnector.cds.client.CdsServerClient;
import energy.eddie.regionconnector.cds.client.CdsServerClientFactory;
import energy.eddie.regionconnector.cds.dtos.CdsServerMasterData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CdsMasterDataTest {
    @Mock
    private CdsServerClientFactory factory;
    @Mock
    private CdsServerClient client;
    @InjectMocks
    private CdsMasterData cdsMasterData;

    @Test
    void testPermissionAdministrators_returnsAllPermissionAdministrators() {
        // Given
        when(factory.getAll()).thenReturn(Flux.just(client));
        when(client.masterData()).thenReturn(createCdsServerMasterData());
        var expected = new PermissionAdministrator("us",
                                                   "CDS Server",
                                                   "CDS Server",
                                                   "1",
                                                   "http://localhost",
                                                   "cds");

        // When
        var res = cdsMasterData.permissionAdministrators();

        // Then
        assertThat(res)
                .singleElement()
                .isEqualTo(expected);
    }

    @Test
    void testGetPermissionAdministrator_returnsPermissionAdministrator() {
        // Given
        when(factory.get(1L)).thenReturn(Optional.of(client));
        when(client.masterData()).thenReturn(createCdsServerMasterData());
        var expected = new PermissionAdministrator("us",
                                                   "CDS Server",
                                                   "CDS Server",
                                                   "1",
                                                   "http://localhost",
                                                   "cds");

        // When
        var res = cdsMasterData.getPermissionAdministrator("1");

        // Then
        assertThat(res)
                .isPresent()
                .contains(expected);
    }

    @Test
    void testMeteredDataAdministrators_returnsAllMeteredDataAdministrators() {
        // Given
        when(factory.getAll()).thenReturn(Flux.just(client));
        when(client.masterData()).thenReturn(createCdsServerMasterData());
        var expected = new MeteredDataAdministrator("us",
                                                   "CDS Server",
                                                   "1",
                                                   "http://localhost",
                                                    "http://localhost",
                                                   "1");

        // When
        var res = cdsMasterData.meteredDataAdministrators();

        // Then
        assertThat(res)
                .singleElement()
                .isEqualTo(expected);
    }

    @Test
    void testGetMeteredDataAdministrator_returnsPermissionAdministrator() {
        // Given
        when(factory.get(1L)).thenReturn(Optional.of(client));
        when(client.masterData()).thenReturn(createCdsServerMasterData());
        var expected = new MeteredDataAdministrator("us",
                                                    "CDS Server",
                                                    "1",
                                                    "http://localhost",
                                                    "http://localhost",
                                                    "1");

        // When
        var res = cdsMasterData.getMeteredDataAdministrator("1");

        // Then
        assertThat(res)
                .isPresent()
                .contains(expected);
    }

    private static Mono<CdsServerMasterData> createCdsServerMasterData() {
        return Mono.just(
                new CdsServerMasterData(
                        "CDS Server",
                        "1",
                        URI.create("http://localhost"),
                        Set.of(new Coverage(EnergyType.ELECTRICITY, "us"))
                )
        );
    }
}