// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.exceptions;

public class MqttTopicException extends Exception {
    public MqttTopicException(String message) {
        super(message);
    }
}
