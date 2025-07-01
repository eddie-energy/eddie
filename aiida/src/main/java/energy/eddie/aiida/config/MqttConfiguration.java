package energy.eddie.aiida.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ConfigurationProperties(prefix = "aiida.mqtt")
public record MqttConfiguration(
        String internalHost,
        String externalHost,
        int bCryptSaltRounds,
        String username,
        String password,
        String tlsCertificatePath
) {
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder(bCryptSaltRounds());
    }
}
