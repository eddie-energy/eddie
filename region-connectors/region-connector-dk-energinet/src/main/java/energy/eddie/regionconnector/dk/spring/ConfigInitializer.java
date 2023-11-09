package energy.eddie.regionconnector.dk.spring;

import org.eclipse.microprofile.config.Config;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class ConfigInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private final Config config;

    public ConfigInitializer(Config config) {
        this.config = config;
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        applicationContext.getBeanFactory().registerSingleton("mpConfig", config);
    }
}