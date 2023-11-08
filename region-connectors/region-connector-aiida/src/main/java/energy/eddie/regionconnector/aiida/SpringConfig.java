package energy.eddie.regionconnector.aiida;

import energy.eddie.api.v0.RegionConnector;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

@SpringBootApplication
public class SpringConfig {
    @Nullable
    private static ConfigurableApplicationContext ctx;

    public static synchronized RegionConnector start() {
        if (ctx == null) {
            var app = new SpringApplicationBuilder(SpringConfig.class)
                    .build();
            // These arguments are needed, since this spring instance tries to load the data needs configs of the core configuration.
            // Random port for this spring application, subject to change in GH-109
            ctx = app.run("--spring.config.import=", "--import.config.file=", "--server.port=0");
        }
        var factory = ctx.getBeanFactory();
        return factory.getBean(RegionConnector.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringConfig.class, args);
    }

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public RegionConnector regionConnector(@Value("${server.port:0}") int port) {
        return new AiidaRegionConnector(port);
    }
}
