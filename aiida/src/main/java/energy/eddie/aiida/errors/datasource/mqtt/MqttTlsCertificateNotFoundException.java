// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.errors.datasource.mqtt;

public class MqttTlsCertificateNotFoundException extends Exception {
    public MqttTlsCertificateNotFoundException(String message) {
        super(message);
    }
}
