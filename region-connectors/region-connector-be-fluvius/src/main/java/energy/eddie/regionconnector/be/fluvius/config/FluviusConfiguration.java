// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.net.URI;

@ConfigurationProperties("region-connector.be.fluvius")
public class FluviusConfiguration {
    private final String baseUrl;
    private final String subscriptionKey;
    private final String contractNumber;
    private final boolean mockMandates;

    public FluviusConfiguration(
            String baseUrl,
            String subscriptionKey,
            String contractNumber,
            boolean mockMandates
    ) {
        if (mockMandates && URI.create(baseUrl).getPath().startsWith("/esco-live")) {
            throw new IllegalArgumentException("Fluvius mock mandates are only available on the sandbox API");
        }
        this.baseUrl = baseUrl;
        this.subscriptionKey = subscriptionKey;
        this.contractNumber = contractNumber;
        this.mockMandates = mockMandates;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String subscriptionKey() {
        return subscriptionKey;
    }

    public String contractNumber() {
        return contractNumber;
    }

    public boolean mockMandates() {
        return mockMandates;
    }
}
