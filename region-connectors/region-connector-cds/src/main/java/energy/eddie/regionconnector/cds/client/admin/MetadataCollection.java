package energy.eddie.regionconnector.cds.client.admin;

import energy.eddie.regionconnector.cds.client.CdsPublicApis;
import energy.eddie.regionconnector.cds.exceptions.CoverageNotSupportedException;
import energy.eddie.regionconnector.cds.exceptions.OAuthNotSupportedException;
import energy.eddie.regionconnector.cds.openapi.model.CarbonDataSpec200Response;
import energy.eddie.regionconnector.cds.openapi.model.Coverages200ResponseAllOfCoverageEntriesInner;
import energy.eddie.regionconnector.cds.openapi.model.OAuthAuthorizationServer200Response;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;

import java.net.URI;
import java.util.List;

@Component
public class MetadataCollection {
    private final CdsPublicApis cdsPublicApis;

    public MetadataCollection(CdsPublicApis cdsPublicApis) {this.cdsPublicApis = cdsPublicApis;}

    /**
     * Collects all the required metadata from a new CDS server.
     * The data includes coverage, oauth metadata, and the CDS metadata
     * @param cdsBaseUri the base URI of the cds server
     * @return a tuple containing the metadata or throws {@link CoverageNotSupportedException} or {@link OAuthNotSupportedException}
     */
    public Mono<Tuple3<CarbonDataSpec200Response, OAuthAuthorizationServer200Response, List<Coverages200ResponseAllOfCoverageEntriesInner>>> metadata(
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
                                         return Mono.zip(
                                                 Mono.just(cdsResponse),
                                                 cdsPublicApis.oauthMetadataSpec(cdsResponse.getOauthMetadata()),
                                                 cdsPublicApis.coverage(cdsResponse.getCoverage())
                                         );
                                     }
                            );
    }
}
