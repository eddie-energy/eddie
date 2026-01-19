package energy.eddie.aiida.services.secrets;

import energy.eddie.aiida.errors.SecretDeletionException;
import energy.eddie.aiida.errors.SecretLoadingException;
import energy.eddie.aiida.errors.SecretStoringException;

import java.util.UUID;

public interface SecretsService {
    void storeSecret(UUID id, SecretType type, String secret) throws SecretStoringException;

    String loadSecret(String alias) throws SecretLoadingException;

    void deleteSecret(String alias) throws SecretDeletionException;
}
