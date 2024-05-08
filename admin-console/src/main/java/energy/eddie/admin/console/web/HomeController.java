package energy.eddie.admin.console.web;

import energy.eddie.admin.console.data.StatusMessage;
import energy.eddie.admin.console.data.StatusMessageRepository;
import energy.eddie.cim.v0_82.cmd.StatusTypeList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;
import java.util.List;

@Controller
public class HomeController {

    private final StatusMessageRepository statusMessageRepository;

    private static final List<String> NON_TERMINATABLE_STATUSES = Arrays.asList(
            "Cancelled",
            "Deactivated",
            "No longer available",
            "Withdrawn",
            "Deactivation",
            "Close",
            "Stop",
            "Not satisfied",
            "Rejected",
            "MALFORMED",
            "UNABLE_TO_SEND",
            "TIMED_OUT",
            "REVOKED",
            "TERMINATED",
            "UNFULFILLABLE",
            "PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT"
    );

    @Autowired
    public HomeController(StatusMessageRepository statusMessageRepository) {
        this.statusMessageRepository = statusMessageRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<StatusMessage> statusMessages = statusMessageRepository.findLatestStatusMessageForAllPermissions();

        //Changes the status type codes (e.g. A05) to the corresponding display value (e.g. "Active")
        List<String> statusDisplays = statusMessages.stream()
                .map(statusMessage -> {
                    try {
                        return StatusTypeList.valueOf(statusMessage.getStatus()).value();
                    } catch (IllegalArgumentException e) {
                        return "UNKNOWN_STATUS";
                    }
                })
                .toList();

        model.addAttribute("title", "Permissions for Service Comparor");
        model.addAttribute("statusMessages", statusMessages);
        model.addAttribute("statusDisplays", statusDisplays);
        model.addAttribute("nonTerminatableStatuses", NON_TERMINATABLE_STATUSES);

        return "index";
    }
}