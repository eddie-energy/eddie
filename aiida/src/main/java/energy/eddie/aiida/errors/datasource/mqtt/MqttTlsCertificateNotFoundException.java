package energy.eddie.aiida.errors.datasource.mqtt;

public class MqttTlsCertificateNotFoundException extends Exception {
    public MqttTlsCertificateNotFoundException(String message) {
        super(message);
    }
}
