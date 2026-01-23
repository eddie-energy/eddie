// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("region-connector.fi.fingrid")
public class FingridConfiguration {
    private final String organisationUser;
    private final String organisationName;
    private final String apiUrl;

    public FingridConfiguration(String organisationUser, String organisationName, String apiUrl) {
        this.organisationUser = organisationUser;
        this.organisationName = organisationName;
        this.apiUrl = apiUrl;
    }

    public String organisationUser() {
        return organisationUser;
    }

    public String organisationName() {
        return organisationName;
    }

    public String apiUrl() {
        return apiUrl;
    }
}
