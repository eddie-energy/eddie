// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core;

import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.core.dtos.SimpleDataSourceInformation;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class MapperConfig {
    @Bean
    public JsonMapperBuilderCustomizer jsonCustomizer() {
        var mod = new SimpleModule()
                .addAbstractTypeMapping(DataSourceInformation.class, SimpleDataSourceInformation.class);
        return builder -> builder.addModule(mod);
    }
}
