// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.errors.datasource.DataSourceNotFoundException;
import energy.eddie.aiida.errors.image.ImageFormatException;
import energy.eddie.aiida.errors.image.ImageNotFoundException;
import energy.eddie.aiida.errors.image.ImageReadException;
import energy.eddie.aiida.services.DataSourceImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/datasources/images")
@Tag(name = "Data Source Images")
public class DataSourceImageController {
    private final DataSourceImageService dataSourceImageService;

    @Autowired
    public DataSourceImageController(DataSourceImageService dataSourceImageService) {
        this.dataSourceImageService = dataSourceImageService;
    }

    @Operation(summary = "Get data source image with data source ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Data source or image not found")
    })
    @GetMapping(path = "/{dataSourceId}")
    public ResponseEntity<byte[]> imageByDataSourceId(@PathVariable UUID dataSourceId) {
        try {
            var image = dataSourceImageService.imageByDataSourceId(dataSourceId);

            return ResponseEntity.status(HttpStatus.OK)
                                 .contentType(MediaType.parseMediaType(image.contentType()))
                                 .body(image.data());
        } catch (ImageNotFoundException | DataSourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @Operation(summary = "Update data source image")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "400", description = "Cannot read image file"),
            @ApiResponse(responseCode = "404", description = "Data source not found")
    })
    @PostMapping(path = "/{dataSourceId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public void updateImage(
            @PathVariable UUID dataSourceId,
            @RequestBody MultipartFile file
    ) throws DataSourceNotFoundException, ImageReadException, ImageFormatException {
        dataSourceImageService.updateImage(dataSourceId, file);
    }

    @Operation(summary = "Delete data source image")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "404", description = "Data source or image not found")
    })
    @DeleteMapping(path = "/{dataSourceId}")
    public void deleteImage(@PathVariable UUID dataSourceId) throws DataSourceNotFoundException, ImageNotFoundException {
        dataSourceImageService.deleteImage(dataSourceId);
    }
}
