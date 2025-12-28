package energy.eddie.regionconnector.de.eta;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DeDataSourceInformationTest {

    @Test
    void testDataSourceInformationValues() {
        DeDataSourceInformation dsi = new DeDataSourceInformation();
        assertThat(dsi.countryCode()).isEqualTo("DE");
        assertThat(dsi.regionConnectorId()).isEqualTo("de-eta");
        assertThat(dsi.permissionAdministratorId()).isEqualTo("eta-plus");
        assertThat(dsi.meteredDataAdministratorId()).isEqualTo("eta-plus");
    }
}
