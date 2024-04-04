package eddie.energy.europeanmasterdata;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
@SpringBootApplication
public class EuropeanMasterDataSpringConfig {

    @Bean
    public GroupedOpenApi europeanMasterDataApi() {
        return GroupedOpenApi
                .builder()
                .group("european-masterdata-controller")
                .displayName("European Master Data API")
                .pathsToMatch("/api/**")
                .build();
    }
}
