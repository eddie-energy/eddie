package energy.eddie.api.agnostic.aiida;

public record MqttDto(String serverUri,
                      String username,
                      String password,
                      String dataTopic,
                      String statusTopic,
                      String terminationTopic) {
}
