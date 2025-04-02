package energy.eddie.outbound.shared.serde;

import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.cim.v0_91_08.retransmission.RTREnveloppe;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class RetransmissionRequestMapper {
    private static final DateTimeFormatter ESMP_DATE_TIME_MINUTE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'");
    private final RTREnveloppe rtrEnvelope;

    public RetransmissionRequestMapper(RTREnveloppe rtrEnvelope) {this.rtrEnvelope = rtrEnvelope;}

    public RetransmissionRequest toRetransmissionRequest() {
        var interval = rtrEnvelope.getMarketDocumentPeriodTimeInterval();
        return
                new RetransmissionRequest(
                        rtrEnvelope.getMessageDocumentHeaderMetaInformationRegionConnector(),
                        rtrEnvelope.getMessageDocumentHeaderMetaInformationPermissionId(),
                        LocalDate.parse(interval.getStart(), ESMP_DATE_TIME_MINUTE_FORMATTER),
                        LocalDate.parse(interval.getEnd(), ESMP_DATE_TIME_MINUTE_FORMATTER)
                );
    }
}
