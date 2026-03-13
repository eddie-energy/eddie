package energy.eddie.regionconnector.de.eta.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class DeEtaPlusConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EnableConfig.class);

    @Configuration
    @EnableConfigurationProperties(DeEtaPlusConfiguration.class)
    static class EnableConfig {}


    @Test
    void constructor_allFieldsAreCorrectlyAssigned() {
        DeEtaPlusConfiguration config = new DeEtaPlusConfiguration(
                "party-id",
                "https://custom.api.de",
                "client-id-123",
                "secret-abc",
                "/custom/metered-data",
                "/custom/permissions/{id}"
        );

        assertThat(config.eligiblePartyId()).isEqualTo("party-id");
        assertThat(config.apiBaseUrl()).isEqualTo("https://custom.api.de");
        assertThat(config.apiClientId()).isEqualTo("client-id-123");
        assertThat(config.apiClientSecret()).isEqualTo("secret-abc");
        assertThat(config.meteredDataEndpoint()).isEqualTo("/custom/metered-data");
        assertThat(config.permissionCheckEndpoint()).isEqualTo("/custom/permissions/{id}");
    }

    @Test
    void springBinding_allPropertiesProvided_allValuesAreCorrect() {
        contextRunner
                .withPropertyValues(
                        "region-connector.de.eta.eligible-party-id=my-party",
                        "region-connector.de.eta.api-base-url=https://test.eta.de",
                        "region-connector.de.eta.api-client-id=my-client",
                        "region-connector.de.eta.api-client-secret=my-secret",
                        "region-connector.de.eta.metered-data-endpoint=/v2/metered",
                        "region-connector.de.eta.permission-check-endpoint=/v2/perms/{id}"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DeEtaPlusConfiguration.class);
                    DeEtaPlusConfiguration config = context.getBean(DeEtaPlusConfiguration.class);
                    assertThat(config.eligiblePartyId()).isEqualTo("my-party");
                    assertThat(config.apiBaseUrl()).isEqualTo("https://test.eta.de");
                    assertThat(config.apiClientId()).isEqualTo("my-client");
                    assertThat(config.apiClientSecret()).isEqualTo("my-secret");
                    assertThat(config.meteredDataEndpoint()).isEqualTo("/v2/metered");
                    assertThat(config.permissionCheckEndpoint()).isEqualTo("/v2/perms/{id}");
                });
    }

    @Test
    void springBinding_onlyRequiredProperties_defaultsAppliedForOptional() {
        contextRunner
                .withPropertyValues(
                        "region-connector.de.eta.eligible-party-id=my-party",
                        "region-connector.de.eta.api-client-id=my-client",
                        "region-connector.de.eta.api-client-secret=my-secret"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DeEtaPlusConfiguration.class);
                    DeEtaPlusConfiguration config = context.getBean(DeEtaPlusConfiguration.class);
                    assertThat(config.apiBaseUrl()).isEqualTo("https://api.eta-plus.de");
                    assertThat(config.meteredDataEndpoint()).isEqualTo("/api/v1/metered-data");
                    assertThat(config.permissionCheckEndpoint()).isEqualTo("/api/v1/permissions/{id}");
                });
    }

    @Test
    @SuppressWarnings("NullAway")
    void springBinding_missingEligiblePartyId_bindsAsNull() {
        contextRunner
                .withPropertyValues(
                        "region-connector.de.eta.api-client-id=my-client",
                        "region-connector.de.eta.api-client-secret=my-secret"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DeEtaPlusConfiguration.class);
                    DeEtaPlusConfiguration config = context.getBean(DeEtaPlusConfiguration.class);
                    assertThat(config.eligiblePartyId()).isNull();
                });
    }

    @Test
    @SuppressWarnings("NullAway")
    void springBinding_missingApiClientId_bindsAsNull() {
        contextRunner
                .withPropertyValues(
                        "region-connector.de.eta.eligible-party-id=my-party",
                        "region-connector.de.eta.api-client-secret=my-secret"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DeEtaPlusConfiguration.class);
                    DeEtaPlusConfiguration config = context.getBean(DeEtaPlusConfiguration.class);
                    assertThat(config.apiClientId()).isNull();
                });
    }

    @Test
    @SuppressWarnings("NullAway")
    void springBinding_missingApiClientSecret_bindsAsNull() {
        contextRunner
                .withPropertyValues(
                        "region-connector.de.eta.eligible-party-id=my-party",
                        "region-connector.de.eta.api-client-id=my-client"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DeEtaPlusConfiguration.class);
                    DeEtaPlusConfiguration config = context.getBean(DeEtaPlusConfiguration.class);
                    assertThat(config.apiClientSecret()).isNull();
                });
    }
}
