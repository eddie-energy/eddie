package energy.eddie.core.masterdata;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MasterDataController {

    private final MasterDataService masterDataService;

    public MasterDataController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    @RequestMapping("/permission-administrators")
    public List<PermissionAdministrator> getPermissionAdministrators() {
        return masterDataService.getPermissionAdministrators();
    }

    @RequestMapping("/permission-administrators/{id}")
    public ResponseEntity<PermissionAdministrator> getPermissionAdministrator(@PathVariable String id) {
        return ResponseEntity.of(masterDataService.getPermissionAdministrator(id));
    }

    @RequestMapping("/metered-data-administrators")
    public List<MeteredDataAdministrator> getMeteredDataAdministrators() {
        return masterDataService.getMeteredDataAdministrators();
    }

    @RequestMapping("/metered-data-administrators/{id}")
    public ResponseEntity<MeteredDataAdministrator> getMeteredDataAdministrator(@PathVariable String id) {
        return ResponseEntity.of(masterDataService.getMeteredDataAdministrator(id));
    }
}
