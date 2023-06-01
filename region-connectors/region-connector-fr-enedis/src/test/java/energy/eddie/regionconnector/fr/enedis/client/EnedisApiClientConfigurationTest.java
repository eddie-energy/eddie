package energy.eddie.regionconnector.fr.enedis.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EnedisApiClientConfigurationTest {
    @Test
    public void testApiClientConfigurationBuilder() {
        EnedisApiClientConfiguration.Builder builder = new EnedisApiClientConfiguration.Builder();
        builder.withClientId("Test").withClientSecret("Test").withHostname("test.com").withBasePath("https://test.com");
        builder.build();
    }

    @Test
    public void testApiClientConfigurationBuilderMissingAttribute() {
        EnedisApiClientConfiguration.Builder builder = new EnedisApiClientConfiguration.Builder();
        builder.withClientId("Test");
        Assertions.assertThrows(IllegalStateException.class, builder::build);
    }
}
