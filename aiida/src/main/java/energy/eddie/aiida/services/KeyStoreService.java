package energy.eddie.aiida.services;

import energy.eddie.aiida.config.MarketplaceConfiguration;
import energy.eddie.aiida.errors.KeyStoreServiceException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Service
public class KeyStoreService {
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String BC_PROVIDER = "BC";
    private static final String KEY_ALIAS = "federator-aiida-key";
    private final MarketplaceConfiguration marketplaceConfiguration;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public KeyStoreService(MarketplaceConfiguration marketplaceConfiguration) {this.marketplaceConfiguration = marketplaceConfiguration;}

    public void saveKeyPairAndCertificate(PrivateKey privateKey, X509Certificate certificate) throws KeyStoreServiceException {
        try {
            var keyStore = loadOrCreateKeyStore();
            keyStore.setKeyEntry(KEY_ALIAS, privateKey, marketplaceConfiguration.getKeystorePassword(), new Certificate[]{certificate});
            try (FileOutputStream fos = new FileOutputStream(marketplaceConfiguration.getKeystorePath())) {
                keyStore.store(fos, marketplaceConfiguration.getKeystorePassword());
            }
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            throw new KeyStoreServiceException("Error while saving key pair and certificate to key store", e);
        }
    }

    public KeyPair getKeyPair() throws KeyStoreServiceException {
        try {
            var keyStore = loadOrCreateKeyStore();
            var privateKey = (PrivateKey) keyStore.getKey(KEY_ALIAS, marketplaceConfiguration.getKeystorePassword());
            var publicKey = keyStore.getCertificate(KEY_ALIAS).getPublicKey();

            return new KeyPair(publicKey, privateKey);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new KeyStoreServiceException("Error while getting key pair from key store", e);
        }
    }

    public X509Certificate getCertificate() throws KeyStoreServiceException {
        try {
            return (X509Certificate) loadOrCreateKeyStore().getCertificate(KEY_ALIAS);
        } catch (KeyStoreException e) {
            throw new KeyStoreServiceException("Error while getting certificate from key store", e);
        }
    }

    private KeyStore loadOrCreateKeyStore() throws KeyStoreServiceException {
        try {
            var keyStore = KeyStore.getInstance(KEYSTORE_TYPE, BC_PROVIDER);
            try (var fis = new FileInputStream(marketplaceConfiguration.getKeystorePath())) {
                keyStore.load(fis, marketplaceConfiguration.getKeystorePassword());
            } catch (IOException e) {
                keyStore.load(null, marketplaceConfiguration.getKeystorePassword());
            }
            return keyStore;
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException |
                 NoSuchProviderException e) {
            throw new KeyStoreServiceException("Error while loading key store from storage", e);
        }
    }
}
