package energy.eddie.api.v0_82.outbound;

import energy.eddie.api.utils.Pair;
import energy.eddie.cim.v0_82.cmd.ConsentMarketDocument;

import java.util.concurrent.Flow;

public interface TerminationConnector {
    Flow.Publisher<Pair<String, ConsentMarketDocument>> getTerminationMessages();

}
