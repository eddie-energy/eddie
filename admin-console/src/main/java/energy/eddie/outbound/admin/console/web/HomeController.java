package energy.eddie.outbound.admin.console.web;

import energy.eddie.cim.v0_82.pmd.StatusTypeList;
import energy.eddie.outbound.admin.console.data.StatusMessage;
import energy.eddie.outbound.admin.console.data.StatusMessageDTO;
import energy.eddie.outbound.admin.console.data.StatusMessageRepository;
import energy.eddie.outbound.admin.console.services.TerminationAdminConsoleConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import static energy.eddie.outbound.admin.console.config.AdminConsoleSecurityConfig.ADMIN_CONSOLE_BASE_URL;

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

    @GetMapping(value = "/statusMessages", params = {"page", "size"})
    public ResponseEntity<PagedModel<StatusMessageDTO>> statusMessagesPaginated(@RequestParam(defaultValue = "0") int page,
                                                                                @RequestParam(defaultValue = "500") int size) {
        return ResponseEntity.ok(new PagedModel<>(this.statusMessageRepository.findLatestStatusMessageForPaginatedPermissions(PageRequest.of(page, size))
                .map(HomeController::dtoFromStatusMessage)));
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

    @GetMapping("/login")
    public String getLogin() {
        return "login";
    }

    @GetMapping("/**")
    public String index(
            Model model,
            @Value("${eddie.public.url}") String publicUrl,
            @Value("${eddie.management.url}") String managementUrl,
            @Value("${eddie.management.server.urlprefix}") String managementUrlPrefix
    ) {
        model.addAttribute("eddiePublicUrl", publicUrl);
        model.addAttribute("eddieAdminConsoleUrl", managementUrl + ADMIN_CONSOLE_BASE_URL);
        model.addAttribute("eddieManagementUrl", managementUrl);
        model.addAttribute("eddieManagementUrlPrefix", managementUrlPrefix);

        return "index";
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
}