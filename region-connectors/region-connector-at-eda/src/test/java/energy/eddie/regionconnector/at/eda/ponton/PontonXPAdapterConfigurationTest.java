// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PontonXPAdapterConfigurationTest {
    private static final String PREFIX = "region-connector.at.eda.ponton.messenger.";
    private static final String[] REQUIRED_PROPERTIES = {
            PREFIX + "adapter.id=adapter-id",
            PREFIX + "adapter.version=adapter-version",
            PREFIX + "hostname=localhost",
            PREFIX + "port=2600",
            PREFIX + "folder=work-folder"
    };

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EnableConfig.class)
            .withPropertyValues(REQUIRED_PROPERTIES);

    @Configuration
    @EnableConfigurationProperties(PontonXPAdapterConfiguration.class)
    static class EnableConfig {
    }

    @Test
    void omittedConnectionSettings_useDefaults() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            var config = context.getBean(PontonXPAdapterConfiguration.class);
            assertThat(config.inboundConnections()).isEqualTo(1);
            assertThat(config.outboundConnections()).isEqualTo(1);
            assertThat(config.connectionWatchdogInterval()).isEqualTo(Duration.ofMinutes(1));
            assertThat(config.connectionStatusTimeout()).isEqualTo(Duration.ofMinutes(2));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void invalidInboundConnectionCount_failsBinding(String value) {
        contextRunner
                .withPropertyValues(PREFIX + "inbound-connections=" + value)
                .run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void invalidOutboundConnectionCount_failsBinding(String value) {
        contextRunner
                .withPropertyValues(PREFIX + "outbound-connections=" + value)
                .run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0s", "-1s"})
    void invalidWatchdogInterval_failsBinding(String value) {
        contextRunner
                .withPropertyValues(PREFIX + "connection-watchdog.interval=" + value)
                .run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0s", "-1s"})
    void invalidConnectionStatusTimeout_failsBinding(String value) {
        contextRunner
                .withPropertyValues(PREFIX + "connection-watchdog.status-timeout=" + value)
                .run(context -> assertThat(context).hasFailed());
    }
}
