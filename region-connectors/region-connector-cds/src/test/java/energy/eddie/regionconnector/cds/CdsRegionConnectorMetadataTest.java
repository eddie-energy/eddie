package energy.eddie.regionconnector.cds;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.master.data.Coverage;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CdsRegionConnectorMetadataTest {
    @Mock
    private CdsServerRepository repository;
    @InjectMocks
    private CdsRegionConnectorMetadata metadata;

    @Test
    void testCountryCodes_returnsAllCountryCodes() {
        // Given
        when(repository.findAll())
                .thenReturn(List.of(
                        new CdsServerBuilder()
                                .setCoverages(Set.of(
                                        new Coverage(EnergyType.NATURAL_GAS, "us"),
                                        new Coverage(EnergyType.ELECTRICITY, "ca")
                                ))
                                .build(),
                        new CdsServerBuilder()
                                .setCoverages(Set.of(
                                        new Coverage(EnergyType.NATURAL_GAS, "us"),
                                        new Coverage(EnergyType.ELECTRICITY, "at")
                                ))
                                .build()
                ));

        // When
        var res = metadata.countryCodes();
        assertThat(res).containsExactlyInAnyOrder("US", "CA", "AT");
    }

    @Test
    void testSupportedEnergyTypes_returnsEnergyTypes() {
        // Given
        when(repository.findAll())
                .thenReturn(List.of(
                        new CdsServerBuilder()
                                .setCoverages(Set.of(
                                        new Coverage(EnergyType.NATURAL_GAS, "us"),
                                        new Coverage(EnergyType.ELECTRICITY, "ca")
                                ))
                                .build(),
                        new CdsServerBuilder()
                                .setCoverages(Set.of(
                                        new Coverage(EnergyType.NATURAL_GAS, "us"),
                                        new Coverage(EnergyType.HYDROGEN, "at")
                                ))
                                .build()
                ));

        // When
        var res = metadata.supportedEnergyTypes();
        assertThat(res).containsExactlyInAnyOrder(EnergyType.NATURAL_GAS, EnergyType.HYDROGEN, EnergyType.ELECTRICITY);
    }
}