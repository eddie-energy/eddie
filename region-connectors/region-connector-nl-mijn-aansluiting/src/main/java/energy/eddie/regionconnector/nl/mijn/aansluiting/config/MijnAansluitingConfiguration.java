// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.config;

import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(value = "region-connector.nl.mijn.aansluiting")
public record MijnAansluitingConfiguration(
        String continuousKeyId,
        String issuerUrl,
        ClientID continuousClientId,
        Scope continuousScope,
        URI codeboekApi,
        String codeboekApiToken,
        URI redirectUrl
) {
}
