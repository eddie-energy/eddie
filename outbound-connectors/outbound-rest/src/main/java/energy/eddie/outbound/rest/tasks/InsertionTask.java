package energy.eddie.outbound.rest.tasks;

import energy.eddie.outbound.rest.model.ModelWithJsonPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import reactor.core.publisher.Flux;

import java.util.function.Function;

public class InsertionTask<T1, T2 extends ModelWithJsonPayload<T1>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InsertionTask.class);
    private final JpaRepository<T2, Long> repository;
    private final Function<T1, T2> converter;

    public InsertionTask(Flux<T1> flux, JpaRepository<T2, Long> repository, Function<T1, T2> converter) {
        this.repository = repository;
        this.converter = converter;
        flux.subscribe(this::insert);
    }

    public void insert(T1 payload) {
        LOGGER.debug("Inserting {}", payload.getClass().getSimpleName());
        var model = converter.apply(payload);
        repository.save(model);
    }
}
