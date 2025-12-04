package energy.eddie.regionconnector.de.eta;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class DeEtaRegionConnectorMetadataTest {

    private final DeEtaRegionConnectorMetadata metadata = new DeEtaRegionConnectorMetadata();

    @Test
    void supportedDataNeeds_containsExpectedClasses() {
        assertThat(metadata.supportedDataNeeds())
                .contains(ValidatedHistoricalDataDataNeed.class, AccountingPointDataNeed.class);
    }

    @Test
    void supportedEnergyTypes_containsElectricity() {
        assertThat(metadata.supportedEnergyTypes())
                .containsExactly(EnergyType.ELECTRICITY);
    }

    @Test
    void supportedGranularities_and_timezone_remainUnchanged() {
        assertThat(metadata.supportedGranularities())
                .containsExactlyInAnyOrder(Arrays.stream(Granularity.values()).toArray(Granularity[]::new));
        assertThat(metadata.timeZone().getId()).isEqualTo("Europe/Berlin");
    }
}
