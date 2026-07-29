// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messenger;

import java.io.IOException;

class PontonRetryStrategy {
    private final MessengerHealth healthApi;

    PontonRetryStrategy(MessengerHealth healthApi) {this.healthApi = healthApi;}

    boolean isRetryable(Exception sendException) {
        Throwable cause = sendException;
        while (cause != null) {
            if (cause instanceof IOException) {
                return this.healthApi.messengerStatus().ok();
            }
            cause = cause.getCause();
        }
        return false;
    }
}
