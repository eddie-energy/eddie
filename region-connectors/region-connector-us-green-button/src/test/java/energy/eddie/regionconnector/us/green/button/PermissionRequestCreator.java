package energy.eddie.regionconnector.us.green.button;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonPermissionRequest;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class PermissionRequestCreator {
    public static GreenButtonPermissionRequest createPermissionRequest() {
        var now = LocalDate.now(ZoneOffset.UTC);
        return new GreenButtonPermissionRequest("pid",
                                                "cid",
                                                "dnid",
                                                now,
                                                now,
                                                Granularity.PT15M,
                                                PermissionProcessStatus.ACCEPTED,
                                                ZonedDateTime.now(ZoneOffset.UTC),
                                                "US",
                                                "company",
                                                "http://localhost",
                                                "scope",
                                                "1111");
    }
}
