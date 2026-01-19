package energy.eddie.aiida.services.secrets;

import energy.eddie.aiida.errors.SecretDeletionException;
import energy.eddie.aiida.errors.SecretLoadingException;
import energy.eddie.aiida.errors.SecretStoringException;
import org.keycloak.crypto.Algorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("NullAway")
@Service
public class KeyStoreSecretsService implements SecretsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyStoreSecretsService.class);
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String SECRET_ALGORITHM = Algorithm.AES;

    @Value("${aiida.keystore.password}")
    private char[] keyStorePassword;

    @Value("${aiida.keystore.path}")
    private String keyStorePath;

    public static String alias(UUID id, SecretType type) {
        return type + "_" + id;
    }

    @Override
    public String loadSecret(String alias) throws SecretLoadingException {
        try {
            var keyStore = loadKeyStore().orElseThrow(new SecretLoadingException());
            var secretEntry = (KeyStore.SecretKeyEntry) keyStore.getEntry(alias, protectionParameter());

            if (secretEntry == null) {
                throw new SecretLoadingException(alias);
            }

            var secretKey = secretEntry.getSecretKey();

            return new String(secretKey.getEncoded(), StandardCharsets.UTF_8);
        } catch (UnrecoverableEntryException | NoSuchAlgorithmException | KeyStoreException e) {
            throw new SecretLoadingException(alias, e);
        }
    }

    @Override
    public void deleteSecret(String alias) throws SecretDeletionException {
        try {
            var keyStore = loadKeyStore().orElseThrow(new SecretDeletionException());

            if (!keyStore.containsAlias(alias)) {
                LOGGER.warn("Keystore does not contain alias {} ", alias);
                return;
            }

            keyStore.deleteEntry(alias);
            storeKeystore(keyStore);
            LOGGER.info("Successfully deleted secret with alias {}", alias);
        } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException e) {
            throw new SecretDeletionException(alias, e);
        }
    }

    private KeyStore.ProtectionParameter protectionParameter() {
        return new KeyStore.PasswordProtection(keyStorePassword);
    }

    private void storeKeystore(KeyStore keyStore) throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        try (var keyStoreFileStream = new FileOutputStream(keyStorePath)) {
            keyStore.store(keyStoreFileStream, keyStorePassword);
        }
    }

    private Optional<KeyStore> loadKeyStore() {
        try {
            return Optional.of(keyStore());
        } catch (KeyStoreException | IOException e) {
            LOGGER.error("Failed to load key store", e);
        } catch (CertificateException e) {
            LOGGER.error("Failed to load certificate", e);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Failed to load algorithm", e);
        }

        return Optional.empty();
    }

    private KeyStore keyStore() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        var keyStore = KeyStore.getInstance(KEYSTORE_TYPE);

        try (var keyStoreFileStream = new FileInputStream(keyStorePath)) {
            keyStore.load(keyStoreFileStream, keyStorePassword);
            LOGGER.debug("Successfully loaded key store from {}", keyStorePath);
        } catch (FileNotFoundException e) {
            keyStore.load(null, keyStorePassword);
            storeKeystore(keyStore);
            LOGGER.debug("Keystore file not found, created a new one under {}", keyStorePath);
        }

        return keyStore;
    }

    @Override
    public void storeSecret(UUID id, SecretType type, String secret) throws SecretStoringException {
        try {
            var keyStore = loadKeyStore().orElseThrow(new SecretStoringException());

            var secretBytes = secret.getBytes(StandardCharsets.UTF_8);
            if (secretBytes.length == 0) {
                throw new IllegalArgumentException("Secret content cannot be empty.");
            }

            var secretKey = new SecretKeySpec(secretBytes, SECRET_ALGORITHM);
            var secretEntry = new KeyStore.SecretKeyEntry(secretKey);

            keyStore.setEntry(alias(id, type), secretEntry, protectionParameter());
            storeKeystore(keyStore);
        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException e) {
            throw new SecretStoringException(id, e);
        }
    }
}
