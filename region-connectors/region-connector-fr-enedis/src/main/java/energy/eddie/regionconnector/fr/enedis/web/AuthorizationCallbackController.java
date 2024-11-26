package energy.eddie.regionconnector.fr.enedis.web;

import energy.eddie.regionconnector.fr.enedis.services.PermissionRequestService;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import jakarta.annotation.Nullable;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthorizationCallbackController {
    public static final String ATTRIBUTE_NAME = "status";
    private final PermissionRequestService permissionRequestService;

    public AuthorizationCallbackController(PermissionRequestService permissionRequestService) {
        this.permissionRequestService = permissionRequestService;
    }

    @GetMapping(value = "/authorization-callback")
    @SuppressWarnings("NullAway") // NullAway doesnt understand the isBlank checks
    public String authorizationCallback(
            @RequestParam(value = "state", required = false) @Nullable String permissionId,
            @RequestParam(value = "usage_point_id", required = false) @Nullable String usagePointId,
            Model model
    ) {
        if (Strings.isNotBlank(usagePointId) && Strings.isNotBlank(permissionId)) {
            var usagePointIds = StringUtils.delimitedListToStringArray(usagePointId, ";");
            model.addAttribute("usagePointIds", String.join(", ", usagePointIds));

            try {
                permissionRequestService.authorizePermissionRequest(permissionId, usagePointIds);
                permissionRequestService.findDataNeedIdForPermission(permissionId)
                                        .ifPresent(id -> model.addAttribute("dataNeedId", id));

                model.addAttribute(ATTRIBUTE_NAME, "OK");
            } catch (PermissionNotFoundException e) {
                model.addAttribute(ATTRIBUTE_NAME, "ERROR");
            }
        } else {
            model.addAttribute(ATTRIBUTE_NAME, "DENIED");
        }

        return "authorization-callback";
    }
}
