// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.record;

import jakarta.annotation.Nullable;

/**
 * Describes whether an inbound document was usable after application processing.
 */
public record InboundProcessingResult(Status status, @Nullable String reason) {

    public static InboundProcessingResult accepted() {
        return new InboundProcessingResult(Status.ACCEPTED, null);
    }

    public static InboundProcessingResult partiallyAccepted(String reason) {
        return new InboundProcessingResult(Status.PARTIALLY_ACCEPTED, reason);
    }

    public static InboundProcessingResult rejected(String reason) {
        return new InboundProcessingResult(Status.REJECTED, reason);
    }

    public enum Status {
        ACCEPTED,
        PARTIALLY_ACCEPTED,
        REJECTED
    }
}
