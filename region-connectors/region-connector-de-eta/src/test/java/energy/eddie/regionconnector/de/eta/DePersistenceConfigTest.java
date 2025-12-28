package energy.eddie.regionconnector.de.eta;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DePersistenceConfigTest {

    @Test
    void testInstantiation() {
        DePersistenceConfig config = new DePersistenceConfig();
        assertThat(config).isNotNull();
    }
}
