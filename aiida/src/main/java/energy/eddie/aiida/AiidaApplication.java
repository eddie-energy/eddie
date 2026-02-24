// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida;

import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.config.cleanup.CleanupConfiguration;
import energy.eddie.aiida.config.datasource.it.SinapsiAlfaConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        CleanupConfiguration.class,
        MqttConfiguration.class,
        SinapsiAlfaConfiguration.class
})
public class AiidaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiidaApplication.class, args);
    }
}
