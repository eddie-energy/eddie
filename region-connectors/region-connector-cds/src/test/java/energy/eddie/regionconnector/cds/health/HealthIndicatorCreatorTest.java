package energy.eddie.regionconnector.cds.health;

import energy.eddie.regionconnector.cds.client.CdsPublicApis;
import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.persistence.CdsServerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.boot.actuate.health.PingHealthIndicator;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthIndicatorCreatorTest {
    @Mock
    private HealthContributorRegistry registry;
    @Mock
    @SuppressWarnings("unused")
    private CdsPublicApis apis;
    @Mock
    private CdsServerRepository repository;
    @InjectMocks
    private HealthIndicatorCreator creator;
    @Captor
    private ArgumentCaptor<CompositeHealthContributor> compositeCaptor;

    @Test
    void testOnContextRefreshEvent_registersAllCdsServers() {
        // Given
        var cdsServer = new CdsServerBuilder().setId(1L).build();
        when(repository.findAll()).thenReturn(List.of(cdsServer));

        // When
        creator.onApplicationEvent();

        // Then
        verify(registry).registerContributor(eq("region-connector-cds"), compositeCaptor.capture());
        var composite = compositeCaptor.getValue();
        assertThat(composite).singleElement()
                             .extracting(NamedContributor::getName)
                             .isEqualTo("1");
    }

    @Test
    void testRegister_reregistersHealthContributor() {
        // Given
        var cdsServer = new CdsServerBuilder().setId(1L).build();
        when(registry.unregisterContributor("region-connector-cds"))
                .thenReturn(CompositeHealthContributor.fromMap(Map.of("test", new PingHealthIndicator())));

        // When
        creator.register(cdsServer);

        // Then
        verify(registry).registerContributor(eq("region-connector-cds"), compositeCaptor.capture());
        var composite = compositeCaptor.getValue();
        assertThat(composite).hasSize(2);
    }

    @Test
    void testRegister_registersHealthContributor() {
        // Given
        var cdsServer = new CdsServerBuilder().setId(1L).build();
        when(registry.unregisterContributor("region-connector-cds"))
                .thenReturn(null);

        // When
        creator.register(cdsServer);

        // Then
        verify(registry).registerContributor(eq("region-connector-cds"), compositeCaptor.capture());
        var composite = compositeCaptor.getValue();
        assertThat(composite).singleElement()
                             .extracting(NamedContributor::getName)
                             .isEqualTo("1");
    }
}