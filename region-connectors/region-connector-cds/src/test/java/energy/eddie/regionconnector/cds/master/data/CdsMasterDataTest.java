package energy.eddie.regionconnector.cds.master.data;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.agnostic.master.data.MeteredDataAdministrator;
import energy.eddie.api.agnostic.master.data.PermissionAdministrator;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CdsMasterDataTest {
    @Mock
    private CdsServerRepository repository;
    @InjectMocks
    private CdsMasterData cdsMasterData;

    @Test
    void testPermissionAdministrators_returnsAllPermissionAdministrators() {
        // Given
        when(repository.findAll())
                .thenReturn(List.of(createCdsServer()));
        var expected = new PermissionAdministrator("US",
                                                   "CDS Server",
                                                   "CDS Server - http://localhost",
                                                   "null",
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
        when(repository.findById(1L))
                .thenReturn(Optional.of(createCdsServer()));
        var expected = new PermissionAdministrator("US",
                                                   "CDS Server",
                                                   "CDS Server - http://localhost",
                                                   "null",
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
        when(repository.findAll())
                .thenReturn(List.of(createCdsServer()));
        var expected = new MeteredDataAdministrator("US",
                                                    "CDS Server",
                                                    "null",
                                                    "http://localhost",
                                                    "http://localhost",
                                                    "null");

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
        when(repository.findById(1L))
                .thenReturn(Optional.of(createCdsServer()));
        var expected = new MeteredDataAdministrator("US",
                                                    "CDS Server",
                                                    "null",
                                                    "http://localhost",
                                                    "http://localhost",
                                                    "null");

        // When
        var res = cdsMasterData.getMeteredDataAdministrator("1");

        // Then
        assertThat(res)
                .isPresent()
                .contains(expected);
    }
    private static CdsServer createCdsServer() {
        return new CdsServerBuilder().setBaseUri("http://localhost")
                                     .setName("CDS Server")
                                     .setCoverages(Set.of(new Coverage(EnergyType.ELECTRICITY, "US")))
                                     .setAdminClientId("client-id")
                                     .setAdminClientSecret("client-secret")
                                     .setTokenEndpoint("http://localhost")
                                     .setAuthorizationEndpoint(null)
                                     .setParEndpoint(null)
                                     .build();
    }
}