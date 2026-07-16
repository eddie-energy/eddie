// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.secrets;

import energy.eddie.aiida.errors.SecretLoadingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class KeyStoreSecretsServiceTest {
    private static final UUID ID = UUID.fromString("4211ea05-d4ab-48ff-8613-8f4791a56606");

    private KeyStoreSecretsService service;
    private Path keyStorePath;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        service = new KeyStoreSecretsService();
        keyStorePath = tempDir.resolve("keystore.p12");
        ReflectionTestUtils.setField(service, "keyStorePassword", "Test123!".toCharArray());
        ReflectionTestUtils.setField(service, "keyStorePath", keyStorePath.toString());
    }

    @Test
    void shouldCreateKeystoreFileWhenItDoesNotExistYet() throws Exception {
        assertTrue(Files.notExists(keyStorePath));

        service.storeSecret(ID, SecretType.PASSWORD, "secret");

        assertTrue(Files.exists(keyStorePath));
    }

    @Test
    void shouldStoreAndLoadSecret() throws Exception {
        service.storeSecret(ID, SecretType.PASSWORD, "secret");

        var loaded = service.loadSecret(KeyStoreSecretsService.alias(ID, SecretType.PASSWORD));

        assertEquals("secret", loaded);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForEmptySecret() {
        assertThrows(IllegalArgumentException.class, () -> service.storeSecret(ID, SecretType.PASSWORD, ""));
    }

    @Test
    void shouldThrowSecretLoadingExceptionForUnknownAlias() {
        assertThrows(SecretLoadingException.class, () -> service.loadSecret("unknown"));
    }

    @Test
    void shouldDeleteStoredSecret() throws Exception {
        var alias = KeyStoreSecretsService.alias(ID, SecretType.PASSWORD);
        service.storeSecret(ID, SecretType.PASSWORD, "secret");

        service.deleteSecret(alias);

        assertThrows(SecretLoadingException.class, () -> service.loadSecret(alias));
    }

    @Test
    void shouldNotThrowWhenDeletingUnknownAlias() {
        assertDoesNotThrow(() -> service.deleteSecret("unknown"));
    }

    @Test
    void aliasShouldCombineTypeAndId() {
        assertEquals("password_" + ID, KeyStoreSecretsService.alias(ID, SecretType.PASSWORD));
        assertEquals("api_key_" + ID, KeyStoreSecretsService.alias(ID, SecretType.API_KEY));
    }
}
