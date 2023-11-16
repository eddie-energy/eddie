package energy.eddie.regionconnector.dk.spring;

import energy.eddie.regionconnector.dk.SpringConfig;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ConfigInitializerTest {

    @Test
    void configInitializer_registersConfigBean() {
        // Given
        Config config = mock(Config.class);
        ConfigInitializer initializer = new ConfigInitializer(config);

        // When
        ConfigurableApplicationContext context = new SpringApplicationBuilder()
                .sources(SpringConfig.class)
                .initializers(initializer)
                .web(WebApplicationType.NONE)
                // To prevent spring from initializing all the beans at startup
                .lazyInitialization(true)
                .run();

        // Then
        assertTrue(context.containsBean("mpConfig"));

        // Cleanup
        context.close();
    }
}
