// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package eddie.energy.europeanmasterdata;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

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
