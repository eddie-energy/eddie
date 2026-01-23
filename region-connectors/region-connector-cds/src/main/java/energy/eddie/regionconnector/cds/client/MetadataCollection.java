// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.client;

import energy.eddie.regionconnector.cds.exceptions.CoverageNotSupportedException;
import energy.eddie.regionconnector.cds.exceptions.OAuthNotSupportedException;
import energy.eddie.regionconnector.cds.openapi.model.OAuthAuthorizationServer200Response;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
public class MetadataCollection {
    private final CdsPublicApis cdsPublicApis;

    public MetadataCollection(CdsPublicApis cdsPublicApis) {this.cdsPublicApis = cdsPublicApis;}

    /**
     * Collects all the required metadata from a new CDS server.
     *
     * @param cdsBaseUri the base URI of the cds server
     * @return a tuple containing the metadata or throws {@link CoverageNotSupportedException} or {@link OAuthNotSupportedException}
     */
    public Mono<OAuthAuthorizationServer200Response> metadata(
            URI cdsBaseUri
    ) {
        return cdsPublicApis.carbonDataSpec(cdsBaseUri)
                            .flatMap(cdsResponse -> {
                                         if (!cdsResponse.getCapabilities().contains("coverage")) {
                                             return Mono.error(new CoverageNotSupportedException());
                                         }
                                         if (cdsResponse.getOauthMetadata() == null) {
                                             return Mono.error(new OAuthNotSupportedException());
                                         }
                                         return cdsPublicApis.oauthMetadataSpec(cdsResponse.getOauthMetadata());
                                     }
                            );
    }
}
