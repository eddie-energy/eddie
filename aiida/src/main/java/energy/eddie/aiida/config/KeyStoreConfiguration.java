package energy.eddie.aiida.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.keystore")
public class KeyStoreConfiguration {
    private final String path;
    private final char[] password;

    public KeyStoreConfiguration(String path, char[] password) {
        this.path = path;
        this.password = password;
    }

    public String getPath() {
        return path;
    }
    public char[] getPassword() {
        return password;
    }
}
