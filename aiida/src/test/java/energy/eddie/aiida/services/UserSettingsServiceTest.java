// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.models.user.UserSettings;
import energy.eddie.aiida.repositories.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceTest {
    private static final UUID USER_ID = UUID.fromString("092bf5cb-8571-4313-9429-8caf5e679f6e");

    @Mock
    private UserSettingsRepository userSettingsRepository;
    @Mock
    private AuthService authService;

    private UserSettingsService service;

    @BeforeEach
    void setUp() {
        service = new UserSettingsService(userSettingsRepository, authService);
    }

    @Test
    void givenNoSettings_getUserSettings_returnsSettingsWithoutEmail() throws Exception {
        when(authService.getCurrentUserId()).thenReturn(USER_ID);
        when(userSettingsRepository.findById(USER_ID)).thenReturn(Optional.empty());

        var result = service.getUserSettings();

        assertEquals(USER_ID, result.userId());
        assertNull(result.contactEmail());
    }

    @Test
    void givenSettings_getUserSettings_returnsSettings() throws Exception {
        when(authService.getCurrentUserId()).thenReturn(USER_ID);
        when(userSettingsRepository.findById(USER_ID))
                .thenReturn(Optional.of(new UserSettings(USER_ID, "user@example.com")));

        var result = service.getUserSettings();

        assertEquals(USER_ID, result.userId());
        assertEquals("user@example.com", result.contactEmail());
    }

    @Test
    void givenNoSettings_updateUserSettings_createsSettings() throws Exception {
        when(authService.getCurrentUserId()).thenReturn(USER_ID);
        when(userSettingsRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(userSettingsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.updateUserSettings("user@example.com");

        assertEquals(USER_ID, result.userId());
        assertEquals("user@example.com", result.contactEmail());

        var captor = ArgumentCaptor.forClass(UserSettings.class);
        verify(userSettingsRepository).save(captor.capture());
        assertEquals("user@example.com", captor.getValue().contactEmail());
    }

    @Test
    void givenSettings_updateUserSettings_changesEmail() throws Exception {
        when(authService.getCurrentUserId()).thenReturn(USER_ID);
        var existing = new UserSettings(USER_ID, "old@example.com");
        when(userSettingsRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
        when(userSettingsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.updateUserSettings("new@example.com");

        assertEquals("new@example.com", result.contactEmail());
        assertEquals("new@example.com", existing.contactEmail());
    }

    @Test
    void givenBlankEmail_updateUserSettings_clearsEmail() throws Exception {
        when(authService.getCurrentUserId()).thenReturn(USER_ID);
        var existing = new UserSettings(USER_ID, "old@example.com");
        when(userSettingsRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
        when(userSettingsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.updateUserSettings("  ");

        assertNull(result.contactEmail());
        verify(userSettingsRepository).save(existing);
    }

    @Test
    void givenContactEmail_findContactEmail_returnsEmail() {
        when(userSettingsRepository.findById(USER_ID))
                .thenReturn(Optional.of(new UserSettings(USER_ID, "user@example.com")));

        assertEquals(Optional.of("user@example.com"), service.findContactEmail(USER_ID));
    }

    @Test
    void givenNoSettings_findContactEmail_returnsEmpty() {
        when(userSettingsRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertTrue(service.findContactEmail(USER_ID).isEmpty());
    }

    @Test
    void givenBlankEmail_findContactEmail_returnsEmpty() {
        when(userSettingsRepository.findById(USER_ID)).thenReturn(Optional.of(new UserSettings(USER_ID, "  ")));

        assertTrue(service.findContactEmail(USER_ID).isEmpty());
    }
}
