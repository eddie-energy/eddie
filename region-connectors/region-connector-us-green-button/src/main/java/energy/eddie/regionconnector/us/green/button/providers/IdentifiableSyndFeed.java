package energy.eddie.regionconnector.us.green.button.providers;

import com.rometools.rome.feed.synd.SyndFeed;
import energy.eddie.api.agnostic.IdentifiablePayload;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;

public record IdentifiableSyndFeed(
        UsGreenButtonPermissionRequest permissionRequest,
        SyndFeed payload
)
        implements IdentifiablePayload<UsGreenButtonPermissionRequest, SyndFeed> {
}
