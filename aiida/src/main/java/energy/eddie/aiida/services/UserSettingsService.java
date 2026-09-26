// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.models.user.UserSettings;
import energy.eddie.aiida.repositories.UserSettingsRepository;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserSettingsService {
    private final UserSettingsRepository userSettingsRepository;
    private final AuthService authService;

    public UserSettingsService(UserSettingsRepository userSettingsRepository, AuthService authService) {
        this.userSettingsRepository = userSettingsRepository;
        this.authService = authService;
    }

    /**
     * Returns the settings of the current user, with null values if no configuration is present.
     */
    public UserSettings getUserSettings() throws InvalidUserException {
        var userId = authService.getCurrentUserId();

        return userSettingsRepository.findById(userId).orElseGet(() -> new UserSettings(userId, null));
    }

    @Transactional(rollbackFor = Exception.class)
    public UserSettings updateUserSettings(@Nullable String contactEmail) throws InvalidUserException {
        var userId = authService.getCurrentUserId();
        var settings = userSettingsRepository.findById(userId).orElseGet(() -> new UserSettings(userId, null));
        settings.setContactEmail(normalize(contactEmail));

        return userSettingsRepository.save(settings);
    }

    public Optional<String> findContactEmail(UUID userId) {
        var settings = userSettingsRepository.findById(userId);
        if (settings.isEmpty()) {
            return Optional.empty();
        }

        var contactEmail = settings.get().contactEmail();
        if (contactEmail == null || contactEmail.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(contactEmail);
    }

    private @Nullable String normalize(@Nullable String contactEmail) {
        if (contactEmail == null || contactEmail.isBlank()) {
            return null;
        }

        return contactEmail.trim();
    }
}
