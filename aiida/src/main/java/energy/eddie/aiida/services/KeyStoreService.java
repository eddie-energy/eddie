package energy.eddie.aiida.services;

import energy.eddie.aiida.config.KeyStoreConfiguration;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

@Service
public class KeyStoreService {
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String PROVIDER = "BC";
    private static final String ALIAS = "federator-aiida-key";
    private final KeyStoreConfiguration keyStoreConfiguration;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public KeyStoreService(KeyStoreConfiguration keyStoreConfiguration) {this.keyStoreConfiguration = keyStoreConfiguration;}

    public void saveKeyPairAndCertificate(PrivateKey privateKey, X509Certificate certificate) {
        try {
            var keyStore = loadOrCreateKeyStore();
            keyStore.setKeyEntry(ALIAS, privateKey, keyStoreConfiguration.getPassword(), new Certificate[]{certificate});
            try (FileOutputStream fos = new FileOutputStream(keyStoreConfiguration.getPath())) {
                keyStore.store(fos, keyStoreConfiguration.getPassword());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while saving to KeyStore", e);
        }
    }

    public KeyPair getKeyPair() {
        try {
            var keyStore = loadOrCreateKeyStore();
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(ALIAS, keyStoreConfiguration.getPassword());
            PublicKey publicKey = keyStore.getCertificate(ALIAS).getPublicKey();

            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            throw new RuntimeException("Error while loading Keypair", e);
        }
    }

    public X509Certificate getCertificate() {
        try {
            return (X509Certificate) loadOrCreateKeyStore().getCertificate(ALIAS);
        } catch (Exception e) {
            throw new RuntimeException("Error while loading Certificate", e);
        }
    }

    private KeyStore loadOrCreateKeyStore() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE, PROVIDER);
            try (var fis = new FileInputStream(keyStoreConfiguration.getPath())) {
                keyStore.load(fis, keyStoreConfiguration.getPassword());
            } catch (Exception e) {
                keyStore.load(null, keyStoreConfiguration.getPassword());
            }

            return keyStore;
        } catch (Exception e) {
            throw new RuntimeException("Error loading KeyStore", e);
        }
    }
}
