package energy.eddie.regionconnector.de.eta.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DeEtaPlusConfiguration}.
 * Verifies default values and validation constraints (@NotBlank, @Positive, @PositiveOrZero, @AssertTrue).
 */
class DeEtaPlusConfigurationTest {

    private static final String CLIENT_ID = "region-connector.de.eta.api-client-id=my-client";
    private static final String CLIENT_SECRET = "region-connector.de.eta.api-client-secret=my-secret";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EnableConfig.class);

    @Configuration
    @EnableConfigurationProperties(DeEtaPlusConfiguration.class)
    static class EnableConfig {
    }

    @Test
    void defaults_areAppliedWhenOnlyRequiredPropertiesSet() {
        contextRunner
                .withPropertyValues(CLIENT_ID, CLIENT_SECRET)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    DeEtaPlusConfiguration config = context.getBean(DeEtaPlusConfiguration.class);
                    assertThat(config.apiBaseUrl()).isEqualTo("https://int.eta-plus.com/api");
                    assertThat(config.meteredDataEndpoint()).isEqualTo("/meters/historical");
                    assertThat(config.permissionCheckEndpoint()).isEqualTo("/v1/permissions/{id}");
                    assertThat(config.responseTimeoutSeconds()).isEqualTo(30);
                    assertThat(config.retryMaxAttempts()).isEqualTo(3);
                    assertThat(config.retryInitialBackoffSeconds()).isEqualTo(2);
                    assertThat(config.sslEnabled()).isTrue();
                    assertThat(config.sslTrustAll()).isFalse();
                });
    }

    @ParameterizedTest(name = "@NotBlank violation: {0}")
    @MethodSource("notBlankViolations")
    void notBlank_violation_contextFails(String description, String[] properties) {
        contextRunner
                .withPropertyValues(properties)
                .run(context -> assertThat(context).hasFailed());
    }

    static Stream<Arguments> notBlankViolations() {
        return Stream.of(
                Arguments.of("api-client-id missing",
                        new String[]{CLIENT_SECRET}),
                Arguments.of("api-client-id empty",
                        new String[]{"region-connector.de.eta.api-client-id=", CLIENT_SECRET}),
                Arguments.of("api-client-id blank",
                        new String[]{"region-connector.de.eta.api-client-id=   ", CLIENT_SECRET}),
                Arguments.of("api-client-secret missing",
                        new String[]{CLIENT_ID}),
                Arguments.of("api-client-secret empty",
                        new String[]{CLIENT_ID, "region-connector.de.eta.api-client-secret="}),
                Arguments.of("api-client-secret blank",
                        new String[]{CLIENT_ID, "region-connector.de.eta.api-client-secret=   "}),
                Arguments.of("api-base-url empty",
                        new String[]{CLIENT_ID, CLIENT_SECRET, "region-connector.de.eta.api-base-url="}),
                Arguments.of("api-base-url blank",
                        new String[]{CLIENT_ID, CLIENT_SECRET, "region-connector.de.eta.api-base-url=   "})
        );
    }

    @ParameterizedTest(name = "{0}={1} → failed={2}")
    @CsvSource({
            "response-timeout-seconds,       1,  false",
            "response-timeout-seconds,       0,  true",
            "response-timeout-seconds,       -1, true",
            "retry-max-attempts,             0,  false",
            "retry-max-attempts,             -1, true",
            "retry-initial-backoff-seconds,  0,  false",
            "retry-initial-backoff-seconds,  -5, true"
    })
    void numericConstraints_enforced(String property, String value, boolean shouldFail) {
        contextRunner
                .withPropertyValues(
                        CLIENT_ID, CLIENT_SECRET,
                        "region-connector.de.eta." + property + "=" + value
                )
                .run(context -> assertContextOutcome(context, shouldFail));
    }

    @ParameterizedTest(name = "url={0}, sslEnabled={1} → failed={2}")
    @CsvSource({
            "https://api.eta-plus.de, true,  false",
            "http://api.eta-plus.de,  true,  true",
            "http://api.eta-plus.de,  false, false",
            "https://api.eta-plus.de, false, true"
    })
    void sslEnabled_mustMatchUrlProtocol(String url, boolean sslEnabled, boolean shouldFail) {
        contextRunner
                .withPropertyValues(
                        CLIENT_ID, CLIENT_SECRET,
                        "region-connector.de.eta.api-base-url=" + url,
                        "region-connector.de.eta.ssl-enabled=" + sslEnabled
                )
                .run(context -> assertContextOutcome(context, shouldFail));
    }

    @Test
    void sslEnabledWithHttpUrl_producesDescriptiveErrorMessage() {
        contextRunner
                .withPropertyValues(
                        CLIENT_ID, CLIENT_SECRET,
                        "region-connector.de.eta.api-base-url=http://api.eta-plus.de",
                        "region-connector.de.eta.ssl-enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("SSL is enabled but the API base URL uses HTTP");
                });
    }

    private static void assertContextOutcome(
            org.springframework.boot.test.context.assertj.AssertableApplicationContext context,
            boolean shouldFail
    ) {
        if (shouldFail) {
            assertThat(context).hasFailed();
        } else {
            assertThat(context).hasNotFailed();
        }
    }
}
