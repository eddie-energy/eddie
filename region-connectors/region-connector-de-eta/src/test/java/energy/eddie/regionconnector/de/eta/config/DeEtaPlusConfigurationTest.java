package energy.eddie.regionconnector.de.eta.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DeEtaPlusConfiguration}.
 * Verifies Spring property binding, default values, and validation constraints.
 */
class DeEtaPlusConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EnableConfig.class);

    @Configuration
    @EnableConfigurationProperties(DeEtaPlusConfiguration.class)
    static class EnableConfig {}

    private static final String CLIENT_ID = "region-connector.de.eta.api-client-id=my-client";
    private static final String CLIENT_SECRET = "region-connector.de.eta.api-client-secret=my-secret";

    // =========================================================================
    // Default-Value-Binding
    // =========================================================================

    @Nested
    class DefaultValueBinding {

        @Test
        void onlyRequiredProperties_allDefaultsApplied() {
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

        @Test
        void allPropertiesProvided_overridesDefaults() {
            contextRunner
                    .withPropertyValues(
                            CLIENT_ID, CLIENT_SECRET,
                            "region-connector.de.eta.eligible-party-id=my-party",
                            "region-connector.de.eta.api-base-url=https://custom.api.de",
                            "region-connector.de.eta.metered-data-endpoint=/v2/metered",
                            "region-connector.de.eta.permission-check-endpoint=/v2/perms/{id}",
                            "region-connector.de.eta.response-timeout-seconds=60",
                            "region-connector.de.eta.retry-max-attempts=5",
                            "region-connector.de.eta.retry-initial-backoff-seconds=10",
                            "region-connector.de.eta.ssl-enabled=true",
                            "region-connector.de.eta.ssl-trust-all=true"
                    )
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        DeEtaPlusConfiguration config = context.getBean(DeEtaPlusConfiguration.class);
                        assertThat(config.eligiblePartyId()).isEqualTo("my-party");
                        assertThat(config.apiBaseUrl()).isEqualTo("https://custom.api.de");
                        assertThat(config.meteredDataEndpoint()).isEqualTo("/v2/metered");
                        assertThat(config.permissionCheckEndpoint()).isEqualTo("/v2/perms/{id}");
                        assertThat(config.responseTimeoutSeconds()).isEqualTo(60);
                        assertThat(config.retryMaxAttempts()).isEqualTo(5);
                        assertThat(config.retryInitialBackoffSeconds()).isEqualTo(10);
                        assertThat(config.sslEnabled()).isTrue();
                        assertThat(config.sslTrustAll()).isTrue();
                    });
        }

        @Test
        void eligiblePartyId_optional_bindsAsNull() {
            contextRunner
                    .withPropertyValues(CLIENT_ID, CLIENT_SECRET)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        DeEtaPlusConfiguration config = context.getBean(DeEtaPlusConfiguration.class);
                        assertThat(config.eligiblePartyId()).isNull();
                    });
        }
    }

    // =========================================================================
    // @NotBlank validation
    // =========================================================================

    @Nested
    class NotBlankValidation {

        @Test
        void apiClientId_missing_fails() {
            contextRunner
                    .withPropertyValues(CLIENT_SECRET)
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        void apiClientId_empty_fails() {
            contextRunner
                    .withPropertyValues("region-connector.de.eta.api-client-id=", CLIENT_SECRET)
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        void apiClientId_blank_fails() {
            contextRunner
                    .withPropertyValues("region-connector.de.eta.api-client-id=   ", CLIENT_SECRET)
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        void apiClientSecret_missing_fails() {
            contextRunner
                    .withPropertyValues(CLIENT_ID)
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        void apiClientSecret_empty_fails() {
            contextRunner
                    .withPropertyValues(CLIENT_ID, "region-connector.de.eta.api-client-secret=")
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        void apiClientSecret_blank_fails() {
            contextRunner
                    .withPropertyValues(CLIENT_ID, "region-connector.de.eta.api-client-secret=   ")
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        void apiBaseUrl_blank_fails() {
            contextRunner
                    .withPropertyValues(
                            CLIENT_ID, CLIENT_SECRET,
                            "region-connector.de.eta.api-base-url=   "
                    )
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        void apiBaseUrl_empty_fails() {
            contextRunner
                    .withPropertyValues(
                            CLIENT_ID, CLIENT_SECRET,
                            "region-connector.de.eta.api-base-url="
                    )
                    .run(context -> assertThat(context).hasFailed());
        }
    }

    // =========================================================================
    // @Positive / @PositiveOrZero validation
    // =========================================================================

    @Nested
    class NumericValidation {

        @Test
        void responseTimeoutSeconds_positiveValue_succeeds() {
            contextRunner
                    .withPropertyValues(
                            CLIENT_ID, CLIENT_SECRET,
                            "region-connector.de.eta.response-timeout-seconds=1"
                    )
                    .run(context -> assertThat(context).hasNotFailed());
        }

        @Test
        void responseTimeoutSeconds_zero_fails() {
            contextRunner
                    .withPropertyValues(
                            CLIENT_ID, CLIENT_SECRET,
                            "region-connector.de.eta.response-timeout-seconds=0"
                    )
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        void responseTimeoutSeconds_negative_fails() {
            contextRunner
                    .withPropertyValues(
                            CLIENT_ID, CLIENT_SECRET,
                            "region-connector.de.eta.response-timeout-seconds=-1"
                    )
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        void retryMaxAttempts_zero_succeeds() {
            contextRunner
                    .withPropertyValues(
                            CLIENT_ID, CLIENT_SECRET,
                            "region-connector.de.eta.retry-max-attempts=0"
                    )
                    .run(context -> assertThat(context).hasNotFailed());
        }

        @Test
        void retryMaxAttempts_negative_fails() {
            contextRunner
                    .withPropertyValues(
                            CLIENT_ID, CLIENT_SECRET,
                            "region-connector.de.eta.retry-max-attempts=-1"
                    )
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        void retryInitialBackoffSeconds_zero_succeeds() {
            contextRunner
                    .withPropertyValues(
                            CLIENT_ID, CLIENT_SECRET,
                            "region-connector.de.eta.retry-initial-backoff-seconds=0"
                    )
                    .run(context -> assertThat(context).hasNotFailed());
        }

        @Test
        void retryInitialBackoffSeconds_negative_fails() {
            contextRunner
                    .withPropertyValues(
                            CLIENT_ID, CLIENT_SECRET,
                            "region-connector.de.eta.retry-initial-backoff-seconds=-5"
                    )
                    .run(context -> assertThat(context).hasFailed());
        }
    }

    // =========================================================================
    // SSL / URL consistency (@AssertTrue)
    // =========================================================================

    @Nested
    class SslUrlConsistency {

        @Test
        void sslEnabled_httpsUrl_succeeds() {
            contextRunner
                    .withPropertyValues(
                            CLIENT_ID, CLIENT_SECRET,
                            "region-connector.de.eta.api-base-url=https://api.eta-plus.de",
                            "region-connector.de.eta.ssl-enabled=true"
                    )
                    .run(context -> assertThat(context).hasNotFailed());
        }

        @Test
        void sslEnabled_httpUrl_fails() {
            contextRunner
                    .withPropertyValues(
                            CLIENT_ID, CLIENT_SECRET,
                            "region-connector.de.eta.api-base-url=http://api.eta-plus.de",
                            "region-connector.de.eta.ssl-enabled=true"
                    )
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        void sslDisabled_httpUrl_succeeds() {
            contextRunner
                    .withPropertyValues(
                            CLIENT_ID, CLIENT_SECRET,
                            "region-connector.de.eta.api-base-url=http://api.eta-plus.de",
                            "region-connector.de.eta.ssl-enabled=false"
                    )
                    .run(context -> assertThat(context).hasNotFailed());
        }

        @Test
        void sslDisabled_httpsUrl_fails() {
            contextRunner
                    .withPropertyValues(
                            CLIENT_ID, CLIENT_SECRET,
                            "region-connector.de.eta.api-base-url=https://api.eta-plus.de",
                            "region-connector.de.eta.ssl-enabled=false"
                    )
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        void sslMismatch_errorMessageIsDescriptive() {
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
    }

    // =========================================================================
    // URL case-sensitivity / edge cases
    // =========================================================================

    @Nested
    class UrlEdgeCases {

        @Test
        void uppercaseHttps_sslEnabled_treatedAsNonHttps() {
            // startsWith("https") is case-sensitive — HTTPS:// won't match
            contextRunner
                    .withPropertyValues(
                            CLIENT_ID, CLIENT_SECRET,
                            "region-connector.de.eta.api-base-url=HTTPS://api.eta-plus.de",
                            "region-connector.de.eta.ssl-enabled=true"
                    )
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        void leadingWhitespaceUrl_sslEnabled_trimmedBySpring() {
            // Spring trims whitespace during property binding, so " https://..." becomes "https://..."
            contextRunner
                    .withPropertyValues(
                            CLIENT_ID, CLIENT_SECRET,
                            "region-connector.de.eta.api-base-url= https://api.eta-plus.de",
                            "region-connector.de.eta.ssl-enabled=true"
                    )
                    .run(context -> assertThat(context).hasNotFailed());
        }
    }

    // =========================================================================
    // Nested config binding (AuthConfig, ApiConfig)
    // =========================================================================

    @Nested
    class NestedConfigBinding {

        @Test
        void authConfig_null_succeeds() {
            contextRunner
                    .withPropertyValues(CLIENT_ID, CLIENT_SECRET)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        DeEtaPlusConfiguration config = context.getBean(DeEtaPlusConfiguration.class);
                        assertThat(config.auth()).isNull();
                    });
        }

        @Test
        void apiConfig_null_succeeds() {
            contextRunner
                    .withPropertyValues(CLIENT_ID, CLIENT_SECRET)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        DeEtaPlusConfiguration config = context.getBean(DeEtaPlusConfiguration.class);
                        assertThat(config.api()).isNull();
                    });
        }

        @Test
        void authConfig_allFieldsProvided_bindsCorrectly() {
            contextRunner
                    .withPropertyValues(
                            CLIENT_ID, CLIENT_SECRET,
                            "region-connector.de.eta.auth.client-id=auth-client",
                            "region-connector.de.eta.auth.client-secret=auth-secret",
                            "region-connector.de.eta.auth.token-url=https://token.url",
                            "region-connector.de.eta.auth.authorization-url=https://auth.url",
                            "region-connector.de.eta.auth.redirect-uri=https://redirect.url",
                            "region-connector.de.eta.auth.scope=metered-data"
                    )
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        DeEtaPlusConfiguration.AuthConfig auth =
                                context.getBean(DeEtaPlusConfiguration.class).auth();
                        assertThat(auth).isNotNull();
                        assertThat(auth.clientId()).isEqualTo("auth-client");
                        assertThat(auth.clientSecret()).isEqualTo("auth-secret");
                        assertThat(auth.tokenUrl()).isEqualTo("https://token.url");
                        assertThat(auth.authorizationUrl()).isEqualTo("https://auth.url");
                        assertThat(auth.redirectUri()).isEqualTo("https://redirect.url");
                        assertThat(auth.scope()).isEqualTo("metered-data");
                    });
        }

        @Test
        void authConfig_partialFields_nullFieldsBindAsNull() {
            contextRunner
                    .withPropertyValues(
                            CLIENT_ID, CLIENT_SECRET,
                            "region-connector.de.eta.auth.client-id=auth-client"
                    )
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        DeEtaPlusConfiguration.AuthConfig auth =
                                context.getBean(DeEtaPlusConfiguration.class).auth();
                        assertThat(auth).isNotNull();
                        assertThat(auth.clientId()).isEqualTo("auth-client");
                        assertThat(auth.clientSecret()).isNull();
                        assertThat(auth.tokenUrl()).isNull();
                    });
        }

        @Test
        void apiConfig_nestedClientConfig_bindsCorrectly() {
            contextRunner
                    .withPropertyValues(
                            CLIENT_ID, CLIENT_SECRET,
                            "region-connector.de.eta.api.client.id=api-id",
                            "region-connector.de.eta.api.client.secret=api-secret"
                    )
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        DeEtaPlusConfiguration.ApiConfig api =
                                context.getBean(DeEtaPlusConfiguration.class).api();
                        assertThat(api).isNotNull();
                        assertThat(api.client()).isNotNull();
                        assertThat(api.client().id()).isEqualTo("api-id");
                        assertThat(api.client().secret()).isEqualTo("api-secret");
                    });
        }

        @Test
        void apiConfig_clientNull_bindsApiWithNullClient() {
            contextRunner
                    .withPropertyValues(CLIENT_ID, CLIENT_SECRET)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        DeEtaPlusConfiguration config = context.getBean(DeEtaPlusConfiguration.class);
                        assertThat(config.api()).isNull();
                    });
        }
    }

    // =========================================================================
    // sslTrustAll combinations
    // =========================================================================

    @Nested
    class SslTrustAllCombinations {

        @Test
        void sslDisabled_trustAllTrue_accepted() {
            // Documenting: sslEnabled=false + sslTrustAll=true is allowed
            // (trustAll is irrelevant when SSL is disabled)
            contextRunner
                    .withPropertyValues(
                            CLIENT_ID, CLIENT_SECRET,
                            "region-connector.de.eta.api-base-url=http://api.eta-plus.de",
                            "region-connector.de.eta.ssl-enabled=false",
                            "region-connector.de.eta.ssl-trust-all=true"
                    )
                    .run(context -> assertThat(context).hasNotFailed());
        }

        @Test
        void sslEnabled_trustAllTrue_accepted() {
            contextRunner
                    .withPropertyValues(
                            CLIENT_ID, CLIENT_SECRET,
                            "region-connector.de.eta.api-base-url=https://api.eta-plus.de",
                            "region-connector.de.eta.ssl-enabled=true",
                            "region-connector.de.eta.ssl-trust-all=true"
                    )
                    .run(context -> assertThat(context).hasNotFailed());
        }
    }
}
