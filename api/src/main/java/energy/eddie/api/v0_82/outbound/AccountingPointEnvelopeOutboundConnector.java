package energy.eddie.api.v0_82.outbound;

import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import reactor.core.publisher.Flux;

/**
 * An outbound connector delivers data to the eligible party. All messages from the region connectors are passed on to
 * the EP using their transport of choice, e.g. Kafka.
 */
public interface AccountingPointEnvelopeOutboundConnector {
    /**
     * Sets the stream of accounting point market documents to be sent to the EP app.
     *
     * @param marketDocumentStream stream of accounting point market documents
     */
    void setAccountingPointEnvelopeStream(
            Flux<AccountingPointEnvelope> marketDocumentStream
    );
}
