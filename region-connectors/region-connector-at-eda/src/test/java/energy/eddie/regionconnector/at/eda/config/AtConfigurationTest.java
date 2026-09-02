// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.config;

import energy.eddie.regionconnector.at.eda.requests.EdaGroupingIdFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class AtConfigurationTest {
    private static final String PREFIX = "region-connector.at.eda.";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EnableConfig.class)
            .withPropertyValues(PREFIX + "eligibleparty.id=EP123456");

    @Test
    void omittedConversationIdPrefix_usesEmptyDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(AtConfiguration.class).conversationIdPrefix()).isEmpty();
        });
    }

    @Test
    void configuredConversationIdPrefix_isBound() {
        contextRunner
                .withPropertyValues(PREFIX + "conversation-id.prefix=DEV1")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(AtConfiguration.class).conversationIdPrefix()).isEqualTo("DEV1");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEV-", "DEV_1", "+"})
    void invalidConversationIdPrefix_failsStartup(String prefix) {
        contextRunner
                .withPropertyValues(PREFIX + "conversation-id.prefix=" + prefix)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("must contain only ASCII letters and digits");
                });
    }

    @Test
    void conversationIdPrefixProducingOverlongGroupingId_failsStartup() {
        contextRunner
                .withPropertyValues(PREFIX + "conversation-id.prefix=12345678901234")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageStartingWith(
                                    "region-connector.at.eda.conversation-id.prefix is too long " +
                                    "for the configured eligible party ID:"
                            )
                            .hasMessageContaining("exceeds the maximum of 35 characters");
                });
    }

    @Test
    void conversationIdPrefixProducingOverlongEnergyCommunityGroupingId_failsStartup() {
        contextRunner
                .withPropertyValues(
                        PREFIX + "eligibleparty.id=EP",
                        PREFIX + "energy-community-id=community",
                        PREFIX + "energy-community-party-id=EC123456",
                        PREFIX + "conversation-id.prefix=12345678901234"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageStartingWith(
                                    "region-connector.at.eda.conversation-id.prefix is too long " +
                                    "for the configured energy community party ID:"
                            )
                            .hasMessageContaining("exceeds the maximum of 35 characters");
                });
    }

    @Test
    void energyCommunityIdWithoutPartyId_failsStartup() {
        contextRunner
                .withPropertyValues(PREFIX + "energy-community-id=community")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void energyCommunityIdWithBlankPartyId_failsStartup() {
        contextRunner
                .withPropertyValues(
                        PREFIX + "energy-community-id=community",
                        PREFIX + "energy-community-party-id="
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void blankEligiblePartyId_failsStartup() {
        contextRunner
                .withPropertyValues(PREFIX + "eligibleparty.id=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AtConfiguration.class)
    @Import(EdaGroupingIdFactory.class)
    static class EnableConfig {
    }
}
