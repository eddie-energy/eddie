package energy.eddie.aiida.aggregator;

import energy.eddie.aiida.models.record.AiidaRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Set;

@Component
public class Aggregator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Aggregator.class);

    public Flux<AiidaRecord> getFilteredFlux(Set<String> allowedCodes) {
        return Flux.empty();
    }
}
