// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.message.streams;

import energy.eddie.spring.regionconnector.extensions.StreamProviderAndSupplierRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageStreamConfig {
    @Bean
    public StreamProviderAndSupplierRegistrar streamProviderAndSupplierRegistrar() {
        return new StreamProviderAndSupplierRegistrar();
    }
}
