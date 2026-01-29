// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages;

import energy.eddie.regionconnector.at.eda.AtEdaBeanConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@TestConfiguration
public class MarshallerConfig {
    @Bean
    @Primary
    public Jaxb2Marshaller jaxb2Marshaller() {
        return new AtEdaBeanConfig().jaxb2Marshaller();
    }
}
