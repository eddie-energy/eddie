package energy.eddie.admin.console.web;

import energy.eddie.admin.console.data.StatusMessage;
import energy.eddie.admin.console.data.StatusMessageDTO;
import energy.eddie.admin.console.data.StatusMessageRepository;
import energy.eddie.cim.v0_82.cmd.StatusTypeList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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

        List<StatusMessageDTO> updatedStatusMessages = statusMessages.stream()
                .map(statusMessage -> {
                    String country = statusMessage.getCountry();
                    if (country.startsWith("N")) {
                        country = country.replaceFirst("N", "");
                    }

                    String status;
                    try {
                        status = StatusTypeList.valueOf(statusMessage.getStatus()).value();
                    } catch (IllegalArgumentException e) {
                        status = "UNKNOWN_STATUS";
                    }

                    return new StatusMessageDTO(
                            country,
                            statusMessage.getDso(),
                            statusMessage.getPermissionId(),
                            statusMessage.getStartDate(),
                            status);
                })
                .toList();

        model.addAttribute("title", "Permissions for Service Comparor");
        model.addAttribute("statusMessages", updatedStatusMessages);
        model.addAttribute("nonTerminatableStatuses", NON_TERMINATABLE_STATUSES);

        return "index";
    }

    @GetMapping("/statusMessages/{permissionId}")
    public ResponseEntity<List<StatusMessageDTO>> getStatusMessages(@PathVariable String permissionId) {
        List<StatusMessage> statusMessages = statusMessageRepository.findByPermissionIdOrderByStartDateDesc(permissionId);

        // Convert the status codes to their display values and map to DTO
        List<StatusMessageDTO> statusMessageDTOs = statusMessages.stream()
                .map(statusMessage -> {
                    String status;
                    try {
                        status = StatusTypeList.valueOf(statusMessage.getStatus()).value();
                    } catch (IllegalArgumentException e) {
                        status = "UNKNOWN_STATUS";
                    }

                    return new StatusMessageDTO(
                            statusMessage.getCountry(),
                            statusMessage.getDso(),
                            statusMessage.getPermissionId(),
                            statusMessage.getStartDate(),
                            status
                    );
                })
                .toList();

        return ResponseEntity.ok(statusMessageDTOs);
    }
}