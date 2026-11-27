// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius;

import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.api.cim.config.PlainCommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.CodingSchemeTypeList;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.be.fluvius.client.FluviusApi;
import energy.eddie.regionconnector.be.fluvius.client.FluviusApiClient;
import energy.eddie.regionconnector.be.fluvius.sandbox.SandboxFluviusApiClient;
import energy.eddie.regionconnector.shared.timeout.CommonTimeoutService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SuppressWarnings("SpringBootApplicationProperties")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "region-connector.be.fluvius.redirect-uri=http://localhost:8080",
        "region-connector.be.fluvius.base-url=https://apihub.fluvius.be/esco-sbx",
        "region-connector.be.fluvius.mock-mandates=true"
})
@Testcontainers
@Import(FluviusSandboxSpringConfigTests.TestConfig.class)
class FluviusSandboxSpringConfigTests {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer("postgres:15-alpine");
    @MockitoBean
    @SuppressWarnings("unused")
    private WebClient webClient;
    @MockitoBean
    @SuppressWarnings("unused")
    private DataNeedsService dataNeedsService;
    @MockitoBean
    @SuppressWarnings("unused")
    private CommonTimeoutService timeoutService;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private FluviusApi fluviusApi;

    @Test
    void mockMandatesEnabled_sandboxClientTakesPrecedence() {
        assertInstanceOf(SandboxFluviusApiClient.class, fluviusApi);
        assertInstanceOf(FluviusApiClient.class, applicationContext.getBean("fluviusApiClient"));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public CommonInformationModelConfiguration cimConfig() {
            return new PlainCommonInformationModelConfiguration(CodingSchemeTypeList.BELGIUM_NATIONAL_CODING_SCHEME,
                                                                "epId");
        }
    }
}
