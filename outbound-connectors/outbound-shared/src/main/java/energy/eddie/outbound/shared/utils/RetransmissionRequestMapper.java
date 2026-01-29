// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.shared.utils;

import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.cim.v0_91_08.RTREnvelope;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class RetransmissionRequestMapper {
    private static final DateTimeFormatter ESMP_DATE_TIME_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'");
    private final RTREnvelope rtrEnvelope;

    public RetransmissionRequestMapper(RTREnvelope rtrEnvelope) {this.rtrEnvelope = rtrEnvelope;}

    public RetransmissionRequest toRetransmissionRequest() {
        var interval = rtrEnvelope.getMarketDocumentPeriodTimeInterval();
        return new RetransmissionRequest(
                        rtrEnvelope.getMessageDocumentHeaderMetaInformationRegionConnector(),
                        rtrEnvelope.getMessageDocumentHeaderMetaInformationPermissionId(),
                        LocalDate.parse(interval.getStart(), ESMP_DATE_TIME_MINUTE_FORMATTER),
                        LocalDate.parse(interval.getEnd(), ESMP_DATE_TIME_MINUTE_FORMATTER)
                );
    }
}
