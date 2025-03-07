package energy.eddie.regionconnector.cds.web;

import energy.eddie.regionconnector.cds.services.oauth.Callback;
import energy.eddie.regionconnector.cds.services.oauth.CallbackService;
import energy.eddie.regionconnector.cds.services.oauth.authorization.AcceptedResult;
import energy.eddie.regionconnector.cds.services.oauth.authorization.UnauthorizedResult;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CallbackController {
    public static final String STATUS = "status";
    private final CallbackService callbackService;

    public CallbackController(CallbackService callbackService) {this.callbackService = callbackService;}

    @GetMapping("/callback")
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public String callback(
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "state") String state,
            Model model
    ) {
        try {
            var res = callbackService.processCallback(new Callback(code, error, state));
            switch (res) {
                case AcceptedResult(String ignoredPermissionId, String dataNeedId) -> {
                    model.addAttribute(STATUS, "OK");
                    model.addAttribute("dataNeedId", dataNeedId);
                }
                case UnauthorizedResult ignored -> model.addAttribute(STATUS, "DENIED");
                case null, default -> model.addAttribute(STATUS, "ERROR");
            }
        } catch (PermissionNotFoundException e) {
            model.addAttribute(STATUS, "ERROR");
        }
        return "authorization-callback";
    }
}
