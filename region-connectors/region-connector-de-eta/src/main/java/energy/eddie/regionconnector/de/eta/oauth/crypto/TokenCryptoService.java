package energy.eddie.regionconnector.de.eta.oauth.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Simple AES-GCM encryption service for storing OAuth tokens at rest.
 * Never logs secrets. Key is provided via configuration.
 */
@Component
public class TokenCryptoService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenCryptoService.class);
    private static final String ALGO = "AES";
    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public TokenCryptoService(
            @Value("${region-connector.de-eta.oauth.crypto-secret:}") String cryptoSecret,
            @Value("${eddie.jwt.hmac.secret:}") String fallbackSecret
    ) {
        String base64 = (cryptoSecret != null && !cryptoSecret.isBlank()) ? cryptoSecret : fallbackSecret;
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        if (keyBytes.length < 16) {
            // Ensure minimal key size; pad deterministically (not ideal, but better than failing hard in default dev)
            byte[] padded = new byte[16];
            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 16));
            keyBytes = padded;
        }
        this.key = new SecretKeySpec(keyBytes, ALGO);
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(cipherText, 0, out, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            LOGGER.warn("Failed to encrypt token (returning null).", e);
            return null;
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null) return null;
        try {
            byte[] all = Base64.getDecoder().decode(ciphertext);
            byte[] iv = new byte[IV_BYTES];
            byte[] ct = new byte[all.length - IV_BYTES];
            System.arraycopy(all, 0, iv, 0, IV_BYTES);
            System.arraycopy(all, IV_BYTES, ct, 0, ct.length);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plain = cipher.doFinal(ct);
            return new String(plain, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.warn("Failed to decrypt token (returning null).", e);
            return null;
        }
    }
}
