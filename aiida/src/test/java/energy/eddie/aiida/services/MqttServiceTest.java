package energy.eddie.aiida.services;

import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.errors.MqttTlsCertificateNotFoundException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.Charset;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;

class MqttServiceTest {
    @Test
    void tlsCertificate_returnsTlsCertificate() throws MqttTlsCertificateNotFoundException, IOException {
        // Given
        var mqttConfiguration = new MqttConfiguration("","", 10, "", "", "src/test/resources/mqtt/cert.pem");
        var mqttService = new MqttService(mqttConfiguration);

        // When
        var certificate = mqttService.tlsCertificate();

        // Then
        assertEquals("MY_SUPER_CERT", certificate.getContentAsString(Charset.defaultCharset()));
    }

    @Test
    void tlsCertificate_noPath_throwsMqttTlsCertificateNotFoundException() {
        // Given
        var mqttConfiguration = new MqttConfiguration("", "", 10, "", "", "");
        var mqttService = new MqttService(mqttConfiguration);

        // When, Then
        assertThrows(MqttTlsCertificateNotFoundException.class, mqttService::tlsCertificate);
    }

    @Test
    void tlsCertificate_fileNotFound_throwsMqttTlsCertificateNotFoundException() {
        // Given
        var mqttConfiguration = new MqttConfiguration("","", 10, "", "", "/path/to/tls/certificate");
        var mqttService = new MqttService(mqttConfiguration);

        // When, Then
        assertThrows(MqttTlsCertificateNotFoundException.class, mqttService::tlsCertificate);
    }
}
