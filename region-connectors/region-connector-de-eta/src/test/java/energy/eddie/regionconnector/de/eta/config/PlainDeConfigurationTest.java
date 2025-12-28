package energy.eddie.regionconnector.de.eta.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class PlainDeConfigurationTest {

    @Test
    void testConfigurationValues() {
        PlainDeConfiguration config = new PlainDeConfiguration();
        
        ReflectionTestUtils.setField(config, "eligiblePartyId", "party-1");
        ReflectionTestUtils.setField(config, "apiBaseUrl", "http://test.url");
        ReflectionTestUtils.setField(config, "apiClientId", "client-id");
        ReflectionTestUtils.setField(config, "apiClientSecret", "client-secret");

        assertThat(config.eligiblePartyId()).isEqualTo("party-1");
        assertThat(config.apiBaseUrl()).isEqualTo("http://test.url");
        assertThat(config.apiClientId()).isEqualTo("client-id");
        assertThat(config.apiClientSecret()).isEqualTo("client-secret");
    }
}
