package energy.eddie.aiida.services;

import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.errors.MqttTlsCertificateNotFoundException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Service
public class MqttService {
    private final MqttConfiguration config;

    public MqttService(MqttConfiguration config) {
        this.config = config;
    }

    public ByteArrayResource tlsCertificate() throws MqttTlsCertificateNotFoundException {
        try {
            var path = config.tlsCertificatePath();
            if(path.isEmpty()) {
                throw new MqttTlsCertificateNotFoundException("Path for TLS certificate for MQTT broker not specified!");
            }

            File file = new File(path);
            byte[] content = Files.readAllBytes(file.toPath());

            return new ByteArrayResource(content);
        } catch (IOException e) {
            throw new MqttTlsCertificateNotFoundException("Cannot read TLS certificate for MQTT broker!");
        }
    }
}
