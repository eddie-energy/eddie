package eddie.energy.europeanmasterdata;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api", produces = "application/json")
@Tag(name = "European Master Data API", description = "Access to permission administrators and metered data administrators")
public class EuropeanMasterDataController {

    private final EuropeanMasterDataService europeanMasterDataService;

    public EuropeanMasterDataController(EuropeanMasterDataService europeanMasterDataService) {
        this.europeanMasterDataService = europeanMasterDataService;
    }

    @Operation(summary = "Get a list of all permission administrators")
    @GetMapping(value = "/permission-administrators")
    public List<PermissionAdministrator> getPermissionAdministrators() {
        return europeanMasterDataService.getPermissionAdministrators();
    }

    @Operation(summary = "Get a single permission administrator by its company ID")
    @GetMapping("/permission-administrators/{id}")
    public ResponseEntity<PermissionAdministrator> getPermissionAdministrator(@PathVariable String id) {
        return ResponseEntity.of(europeanMasterDataService.getPermissionAdministrator(id));
    }

    @Operation(summary = "Get a list of all metered data administrators")
    @GetMapping("/metered-data-administrators")
    public List<MeteredDataAdministrator> getMeteredDataAdministrators() {
        return europeanMasterDataService.getMeteredDataAdministrators();
    }

    @Operation(summary = "Get a single metered data administrator by its company ID")
    @GetMapping("/metered-data-administrators/{id}")
    public ResponseEntity<MeteredDataAdministrator> getMeteredDataAdministrator(@PathVariable String id) {
        return ResponseEntity.of(europeanMasterDataService.getMeteredDataAdministrator(id));
    }
}
