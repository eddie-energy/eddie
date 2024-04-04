package eddie.energy.europeanmasterdata;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "European Master Data API", description = "Access to permission administrators and metered data administrators")
public class EuropeanMasterDataController {

    private final EuropeanMasterDataService europeanMasterDataService;

    public EuropeanMasterDataController(EuropeanMasterDataService europeanMasterDataService) {
        this.europeanMasterDataService = europeanMasterDataService;
    }

    @GetMapping("/permission-administrators")
    public List<PermissionAdministrator> getPermissionAdministrators() {
        return europeanMasterDataService.getPermissionAdministrators();
    }

    @GetMapping("/permission-administrators/{id}")
    public ResponseEntity<PermissionAdministrator> getPermissionAdministrator(@PathVariable String id) {
        return ResponseEntity.of(europeanMasterDataService.getPermissionAdministrator(id));
    }

    @GetMapping("/metered-data-administrators")
    public List<MeteredDataAdministrator> getMeteredDataAdministrators() {
        return europeanMasterDataService.getMeteredDataAdministrators();
    }

    @GetMapping("/metered-data-administrators/{id}")
    public ResponseEntity<MeteredDataAdministrator> getMeteredDataAdministrator(@PathVariable String id) {
        return ResponseEntity.of(europeanMasterDataService.getMeteredDataAdministrator(id));
    }
}
