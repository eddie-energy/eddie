package energy.eddie.aiida.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ConfigurationProperties(prefix = "aiida.mqtt")
public record MqttConfiguration(
        String internalHost,
        String externalHost,
        Integer bCryptSaltRounds,
        String password,
        String tlsCertificatePath
) {
    private static final String USERNAME = "aiida";

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder(bCryptSaltRounds());
    }

    public String username() {
        return USERNAME;
    }
}
