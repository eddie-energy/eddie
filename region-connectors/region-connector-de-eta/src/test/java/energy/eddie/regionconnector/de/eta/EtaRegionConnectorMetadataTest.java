package energy.eddie.regionconnector.de.eta;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EtaRegionConnectorMetadataTest {

    @Test
    void getInstanceShouldReturnSameInstance() {
        var instance1 = EtaRegionConnectorMetadata.getInstance();
        var instance2 = EtaRegionConnectorMetadata.getInstance();
        assertThat(instance1).isSameAs(instance2);
    }

    @Test
    void metadataFieldsShouldBeCorrect() {
        var metadata = EtaRegionConnectorMetadata.getInstance();
        assertThat(metadata.id()).isEqualTo("de-eta");
        assertThat(metadata.countryCode()).isEqualTo("DE");
        assertThat(metadata.countryCodes()).containsExactly("DE");
        assertThat(metadata.coveredMeteringPoints()).isEqualTo(50000000L);
        assertThat(metadata.earliestStart()).isEqualTo(EtaRegionConnectorMetadata.PERIOD_EARLIEST_START);
        assertThat(metadata.latestEnd()).isEqualTo(EtaRegionConnectorMetadata.PERIOD_LATEST_END);
        assertThat(metadata.supportedGranularities()).containsExactly(
                Granularity.PT15M,
                Granularity.PT1H,
                Granularity.P1D
        );
        assertThat(metadata.timeZone()).isEqualTo(EtaRegionConnectorMetadata.DE_ZONE_ID);
        assertThat(metadata.supportedEnergyTypes()).containsExactlyInAnyOrder(
                EnergyType.ELECTRICITY,
                EnergyType.NATURAL_GAS
        );
        assertThat(metadata.supportedDataNeeds()).containsExactlyInAnyOrder(
                ValidatedHistoricalDataDataNeed.class,
                AccountingPointDataNeed.class
        );
    }

    @Test
    void granularitiesForShouldReturnAllSupportedGranularities() {
        var metadata = EtaRegionConnectorMetadata.getInstance();
        assertThat(metadata.granularitiesFor(EnergyType.ELECTRICITY))
                .isEqualTo(metadata.supportedGranularities());
    }
}
