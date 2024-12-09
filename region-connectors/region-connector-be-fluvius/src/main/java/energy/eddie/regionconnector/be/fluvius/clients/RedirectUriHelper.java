package energy.eddie.regionconnector.be.fluvius.clients;

import org.springframework.web.util.UriComponentsBuilder;

import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_ACCEPTED;
import static energy.eddie.regionconnector.shared.web.RestApiPaths.PATH_PERMISSION_REJECTED;

class RedirectUriHelper {
    private final String publicUri;

    RedirectUriHelper(String publicUri) {this.publicUri = publicUri;}


    String successUri(String permissionId) {
        return expandUri(permissionId, PATH_PERMISSION_ACCEPTED);
    }

    String rejectUri(String permissionId) {
        return expandUri(permissionId, PATH_PERMISSION_REJECTED);
    }

    private String expandUri(String permissionId, String path) {
        return UriComponentsBuilder.fromUriString(publicUri)
                                   .pathSegment("region-connectors", "be-fluvius")
                                   .path(path)
                                   .buildAndExpand(permissionId)
                                   .toString();
    }
}
