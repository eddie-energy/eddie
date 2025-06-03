package energy.eddie.regionconnector.cds.client;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

class CarbonDataSpecificationUri {
    private static final String CDS_METADATA_ENDPOINT = "/.well-known/cds-server-metadata.json";

    private CarbonDataSpecificationUri() {
        // No-Op
    }

    public static URI create(URI uri) {
        return UriComponentsBuilder.fromUri(uri)
                                   .path(CDS_METADATA_ENDPOINT)
                                   .build()
                                   .toUri();
    }
}
