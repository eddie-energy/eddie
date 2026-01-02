package energy.eddie.regionconnector.cds;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.regionconnector.cds.client.CdsServerClient;
import energy.eddie.regionconnector.cds.client.CdsServerClientFactory;
import energy.eddie.regionconnector.cds.dtos.CdsServerMasterData;
import energy.eddie.regionconnector.cds.master.data.Coverage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CdsRegionConnectorMetadataTest {
    @Mock
    private CdsServerClientFactory factory;
    @Mock
    private CdsServerClient client;
    @InjectMocks
    private CdsRegionConnectorMetadata metadata;

    @BeforeEach
    void setUp() {
        when(factory.getAll()).thenReturn(Flux.just(client));
    }

    @Test
    void testCountryCodes_returnsAllCountryCodes() {
        // Given
        when(client.masterData())
                .thenReturn(Mono.just(
                        new CdsServerMasterData(
                                "CDS Server",
                                "1",
                                URI.create("http://localhost"),
                                Set.of(
                                        new Coverage(EnergyType.ELECTRICITY, "us"),
                                        new Coverage(EnergyType.ELECTRICITY, "ca"),
                                        new Coverage(EnergyType.ELECTRICITY, "at")
                                )
                        )
                ));

        // When
        var res = metadata.countryCodes();
        assertThat(res).containsExactlyInAnyOrder("us", "ca", "at");
    }
}