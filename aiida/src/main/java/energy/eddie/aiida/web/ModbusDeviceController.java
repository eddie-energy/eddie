package energy.eddie.aiida.web;

import energy.eddie.aiida.models.modbus.Device;
import energy.eddie.aiida.models.modbus.ModbusModel;
import energy.eddie.aiida.models.modbus.ModbusVendor;
import energy.eddie.aiida.services.ModbusDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/datasources/modbus")
@Tag(name = "Modbus Device Controller")
public class ModbusDeviceController {
    private final ModbusDeviceService service;

    @Autowired
    public ModbusDeviceController(ModbusDeviceService service) {
        this.service = service;
    }

    @Operation(summary = "Get available modbus vendors", description = "Retrieve all vendors.",
            operationId = "getAllModbusVendors", tags = {"datasource"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ModbusVendor.class))))
    })
    @GetMapping(path = "/vendors", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ModbusVendor>> getAllModbusVendors() {
        List<ModbusVendor> vendors = service.vendors();

        if (vendors == null || vendors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No vendors found");
        }

        return ResponseEntity.ok(vendors);
    }

    @Operation(summary = "Get available models for vendor", description = "Retrieve all models for a vendor.",
            operationId = "getModelsByVendor", tags = {"datasource"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ModbusModel.class))))
    })
    @GetMapping(path = "/vendors/{vendorId}/models", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ModbusModel>> getModelsByVendor(@PathVariable String vendorId) {
        var models = service.models(vendorId);

        if (models == null || models.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No models found");
        }

        return ResponseEntity.ok(models);
    }

    @Operation(summary = "Get available devices for model", description = "Retrieve all devices for a model.",
            operationId = "getDevicesByModel", tags = {"datasource"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = Device.class))))
    })
    @GetMapping(path = "/models/{modelId}/devices", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Device>> getDevicesByModel(@PathVariable String modelId) {
        var devices = service.devices(modelId);

        if (devices == null || devices.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No devices found");
        }

        return ResponseEntity.ok(devices);
    }

}
