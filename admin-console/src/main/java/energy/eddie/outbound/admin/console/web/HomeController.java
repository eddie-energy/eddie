package energy.eddie.outbound.admin.console.web;

import energy.eddie.cim.v0_82.pmd.StatusTypeList;
import energy.eddie.outbound.admin.console.data.StatusMessage;
import energy.eddie.outbound.admin.console.data.StatusMessageDTO;
import energy.eddie.outbound.admin.console.data.StatusMessageRepository;
import energy.eddie.outbound.admin.console.services.TerminationAdminConsoleConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class HomeController {

    private final StatusMessageRepository statusMessageRepository;
    private final TerminationAdminConsoleConnector terminationConnector;

    @Autowired
    public HomeController(
            StatusMessageRepository statusMessageRepository,
            TerminationAdminConsoleConnector terminationConnector
    ) {
        this.statusMessageRepository = statusMessageRepository;
        this.terminationConnector = terminationConnector;
    }

    @GetMapping("/statusMessages")
    public ResponseEntity<List<StatusMessageDTO>> statusMessagesList() {
        return ResponseEntity.ok(this.statusMessageRepository.findLatestStatusMessageForAllPermissions()
                                                             .stream()
                                                             .map(HomeController::dtoFromStatusMessage)
                                                             .toList());
    }

    @GetMapping("/statusMessages/{permissionId}")
    public ResponseEntity<List<StatusMessageDTO>> getStatusMessages(@PathVariable String permissionId) {
        List<StatusMessage> statusMessages = statusMessageRepository.findByPermissionIdOrderByStartDateDescIdDesc(
                permissionId);

        List<StatusMessageDTO> statusMessageDTOs = statusMessages.stream()
                .map(HomeController::dtoFromStatusMessage)
                .toList();

        return ResponseEntity.ok(statusMessageDTOs);
    }

    @PostMapping("/terminate/{permissionId}")
    public ResponseEntity<Void> terminatePermission(@PathVariable String permissionId) {
        var statusMessages = statusMessageRepository.findByPermissionIdOrderByStartDateDescIdDesc(permissionId);
        var statusMessage = statusMessages.getFirst();
        terminationConnector.terminate(statusMessage.getPermissionId(), statusMessage.getRegionConnectorId());
        return ResponseEntity.ok().build();
    }

    private static StatusMessageDTO dtoFromStatusMessage(StatusMessage statusMessage) {
        String country = statusMessage.getCountry();
        if (country.startsWith("N")) {
            country = country.replaceFirst("N", "");
        }

        String cimStatus;
        try {
            cimStatus = StatusTypeList.valueOf(statusMessage.getStatus()).value();
        } catch (IllegalArgumentException e) {
            cimStatus = "UNKNOWN_STATUS";
        }

        return new StatusMessageDTO(
                statusMessage.getPermissionId(),
                statusMessage.getRegionConnectorId(),
                statusMessage.getDataNeedId(),
                country,
                statusMessage.getDso(),
                statusMessage.getStartDate(),
                statusMessage.getDescription(),
                cimStatus);
    }

    @GetMapping("/login")
    public String getLogin() {
        return "login";
    }

    @GetMapping("/**")
    public String index() {
        return "forward:/outbound-connectors/admin-console/index.html";
    }
}