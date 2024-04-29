package energy.eddie.regionconnector.aiida.mqtt;

public record MqttDto(String username,
                      String password,
                      String dataTopic,
                      String statusTopic,
                      String terminationTopic) {
}
