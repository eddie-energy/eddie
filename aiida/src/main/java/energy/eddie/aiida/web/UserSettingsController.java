// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.web;

import energy.eddie.aiida.dtos.user.UpdateUserSettingsDto;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.models.user.UserSettings;
import energy.eddie.aiida.services.UserSettingsService;
import energy.eddie.api.agnostic.EddieApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/user-settings")
@Tag(name = "User Settings Controller")
public class UserSettingsController {
    private final UserSettingsService userSettingsService;

    public UserSettingsController(UserSettingsService userSettingsService) {
        this.userSettingsService = userSettingsService;
    }

    @Operation(summary = "Get user settings",
               description = "Returns the settings of the current user, including the contact email for notifications.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200",
                                        description = "Successful operation",
                                        content = @Content(schema = @Schema(implementation = UserSettings.class))), @ApiResponse(
            responseCode = "401",
            description = "Unauthorized User",
            content = @Content)})
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserSettings> getUserSettings() throws InvalidUserException {
        return ResponseEntity.ok(userSettingsService.getUserSettings());
    }

    @Operation(summary = "Update user settings",
               description = "Updates the settings of the current user. An empty contact email disables notifications.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200",
                                        description = "Successful operation",
                                        content = @Content(schema = @Schema(implementation = UserSettings.class))), @ApiResponse(
            responseCode = "400",
            description = "Invalid contact email",
            content = @Content(schema = @Schema(implementation = EddieApiError.class))), @ApiResponse(responseCode = "401",
                                                                                                      description = "Unauthorized User",
                                                                                                      content = @Content)})
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserSettings> updateUserSettings(
            @Valid @RequestBody UpdateUserSettingsDto updateDto
    ) throws InvalidUserException {
        return ResponseEntity.ok(userSettingsService.updateUserSettings(updateDto.contactEmail()));
    }
}
