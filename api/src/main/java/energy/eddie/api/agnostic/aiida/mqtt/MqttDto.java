// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.aiida.mqtt;

public record MqttDto(String serverUri,
                      String username,
                      String password,
                      String dataTopic,
                      String statusTopic,
                      String terminationTopic) {
}
