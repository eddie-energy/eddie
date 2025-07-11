package energy.eddie.exampleappbackend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {
    @Bean
    public OpenAPI openAPIInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("EDDIE Example App API")
                        .version("v0.0.1")
                );
    }
}
