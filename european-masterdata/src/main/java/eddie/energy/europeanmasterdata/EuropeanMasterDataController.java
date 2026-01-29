// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package eddie.energy.europeanmasterdata;

import energy.eddie.api.agnostic.master.data.MasterDataCollection;
import energy.eddie.api.agnostic.master.data.MeteredDataAdministrator;
import energy.eddie.api.agnostic.master.data.PermissionAdministrator;
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

    private final MasterDataCollection masterDataService;

    public EuropeanMasterDataController(
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // Injected from parent context
            MasterDataCollection masterDataService) {
        this.masterDataService = masterDataService;
    }

    @Operation(summary = "Get a list of all permission administrators")
    @GetMapping(value = "/permission-administrators")
    public List<PermissionAdministrator> getPermissionAdministrators() {
        return masterDataService.getPermissionAdministrators();
    }

    @Operation(summary = "Get a single permission administrator by its company ID")
    @GetMapping("/permission-administrators/{id}")
    public ResponseEntity<PermissionAdministrator> getPermissionAdministrator(@PathVariable String id) {
        return ResponseEntity.of(masterDataService.getPermissionAdministrator(id));
    }

    @Operation(summary = "Get a list of all metered data administrators")
    @GetMapping("/metered-data-administrators")
    public List<MeteredDataAdministrator> getMeteredDataAdministrators() {
        return masterDataService.getMeteredDataAdministrators();
    }

    @Operation(summary = "Get a single metered data administrator by its company ID")
    @GetMapping("/metered-data-administrators/{id}")
    public ResponseEntity<MeteredDataAdministrator> getMeteredDataAdministrator(@PathVariable String id) {
        return ResponseEntity.of(masterDataService.getMeteredDataAdministrator(id));
    }
}
