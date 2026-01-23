// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.datasource.mqtt;

public class MqttUnauthorizedException extends Exception {
    public MqttUnauthorizedException(String message) {
        super(message);
    }
}
