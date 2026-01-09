package energy.eddie.spring.regionconnector.extensions.dataneeds;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.core.services.DataNeedRuleSetRouter;
import energy.eddie.core.services.UnknownRegionConnectorException;
import energy.eddie.dataneeds.rules.DataNeedRuleSet;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataNeedRuleSetRegistrarTest {
    @Mock
    private RegionConnector regionConnector;
    @Mock
    private RegionConnectorMetadata metadata;

    @Test
    void givenDataNeedRuleSet_whenRegistered_thenRegistersWithDataNeedRuleSetRouter() throws UnknownRegionConnectorException {
        // Given
        var router = new DataNeedRuleSetRouter();
        when(regionConnector.getMetadata()).thenReturn(metadata);
        when(metadata.id()).thenReturn("id");

        // When
        new DataNeedRuleSetRegistrar(
                new ObjectProvider<>() {
                    @Override
                    public @NotNull Stream<DataNeedRuleSet> stream() {
                        return Stream.of(List::of);
                    }
                },
                regionConnector,
                router
        );

        // Then
        assertThat(router.dataNeedRuleSets("id")).isNotNull();
    }
}