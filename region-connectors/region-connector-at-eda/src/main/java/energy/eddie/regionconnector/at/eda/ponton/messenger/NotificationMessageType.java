// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messenger;

/**
 * Mapping for all processes steps and message codes for the <a href="https://www.ebutilities.at/schemas/232">CMNotification</a>.
 */
public enum NotificationMessageType {
    CCMO_ACCEPT,
    CCMO_REJECT,
    CCMO_ANSWER,
    CCMS_REJECT,
    CCMS_ANSWER,
    ECON_REJECT,
    ECON_ANSWER,
    ECON_ACCEPT,
    ECON_CANCEL,
    PONTON_ERROR
}
