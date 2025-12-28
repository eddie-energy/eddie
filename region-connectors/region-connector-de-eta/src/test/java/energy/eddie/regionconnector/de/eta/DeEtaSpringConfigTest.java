package energy.eddie.regionconnector.de.eta;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeEtaSpringConfigTest {

    @Test
    void testInstantiation() {
        DeEtaSpringConfig config = new DeEtaSpringConfig();
        assertThat(config).isNotNull();
    }
}
