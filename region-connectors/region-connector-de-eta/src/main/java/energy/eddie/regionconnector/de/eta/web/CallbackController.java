package energy.eddie.regionconnector.de.eta.web;

import energy.eddie.regionconnector.de.eta.persistence.DeEtaPermissionRequestRepository;
import energy.eddie.regionconnector.de.eta.permission.events.AcceptedEvent;
import energy.eddie.regionconnector.de.eta.permission.events.InvalidEvent;
import energy.eddie.regionconnector.de.eta.permission.events.RejectedEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CallbackController {
    public static final String STATUS = "status";
    private static final String AUTHORIZATION_CALLBACK_VIEW = "authorization-callback";

    private final Outbox outbox;
    private final DeEtaPermissionRequestRepository permissionRequestRepository;

    public CallbackController(Outbox outbox, DeEtaPermissionRequestRepository permissionRequestRepository) {
        this.outbox = outbox;
        this.permissionRequestRepository = permissionRequestRepository;
    }

    @GetMapping("/permission-request/callback")
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public String callback(
            @RequestParam(name = "permissionId") String permissionId,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "rejected", required = false) String rejected,
            @RequestParam(name = "reason", required = false) String reason,
            Model model
    ) {
        var maybeReq = permissionRequestRepository.findByPermissionId(permissionId);
        if (maybeReq.isEmpty()) {
            outbox.commit(new InvalidEvent(permissionId, "Permission not found"));
            model.addAttribute(STATUS, "ERROR");
            return AUTHORIZATION_CALLBACK_VIEW;
        }

        var request = maybeReq.get();

        boolean isRejected = rejected != null && (rejected.equalsIgnoreCase("true") || rejected.equalsIgnoreCase("1"));
        if (isRejected) {
            outbox.commit(new RejectedEvent(permissionId));
            model.addAttribute(STATUS, "DENIED");
            return AUTHORIZATION_CALLBACK_VIEW;
        }

        if (error == null || error.isBlank()) {
            outbox.commit(new AcceptedEvent(permissionId));
            model.addAttribute(STATUS, "OK");
            model.addAttribute("dataNeedId", request.dataNeedId());
            return AUTHORIZATION_CALLBACK_VIEW;
        }

        outbox.commit(new InvalidEvent(permissionId, reason == null ? error : reason));
        model.addAttribute(STATUS, "ERROR");
        return AUTHORIZATION_CALLBACK_VIEW;
    }
}
