// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.dataneeds.services;

import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.TimeframedDataNeed;
import energy.eddie.dataneeds.persistence.DataNeedsNameAndIdProjection;
import energy.eddie.dataneeds.persistence.DataNeedsRepository;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@ConditionalOnProperty(value = "eddie.data-needs-config.data-need-source", havingValue = "DATABASE")
@Transactional(value = Transactional.TxType.REQUIRED)
public class DataNeedsDbService implements DataNeedsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataNeedsDbService.class);
    private final DataNeedsRepository repository;

    public DataNeedsDbService(DataNeedsRepository repository) {
        this.repository = repository;
        LOGGER.info("Initialized database data needs service.");
    }

    /**
     * Hibernate always responds with a proxy object instead of the actual implementation.
     * That way it is possible to lazily load parts of the object, but a proxy is not part of the inheritance tree, making it impossible to use the {@code instanceof} operator.
     * This method unpacks the hibernate proxy to its actual implementation.
     *
     * @param <T>    The type of the object to unproxy
     * @param entity The object to unproxy
     * @return the actual object and not the proxy
     */
    @SuppressWarnings("unchecked")
    public static <T> T initializeAndUnproxy(T entity) {
        Hibernate.initialize(entity);
        if (entity instanceof HibernateProxy proxy) {
            entity = (T) proxy.getHibernateLazyInitializer().getImplementation();
        }
        return entity;
    }

    @Override
    public List<DataNeedsNameAndIdProjection> getDataNeedIdsAndNames() {
        return repository.findAllBy()
                         .stream()
                         .map(DataNeedsDbService::initializeAndUnproxy)
                         .toList();
    }

    @Override
    public Optional<DataNeed> findById(String id) {
        return repository.findById(id).map(DataNeedsDbService::initializeAndUnproxy);
    }

    @Override
    public DataNeed getById(String id) {
        return initializeAndUnproxy(repository.getReferenceById(id));
    }

    /**
     * Enables or disables a data need.
     *
     * @param id        The ID of the data need to enable or disable.
     * @param isEnabled If the data need should be enabled or disabled.
     */
    public void enableOrDisableDataNeed(String id, boolean isEnabled) {
        repository.setEnabledById(id, isEnabled);
    }

    /**
     * Saves the new data need in the database and sets a UUID as {@link DataNeed#id()}.
     *
     * @param newDataNeed Data need to save in the database.
     * @return Persisted data need.
     */
    public DataNeed saveNewDataNeed(DataNeed newDataNeed) {
        newDataNeed.setId(UUID.randomUUID().toString());
        newDataNeed.regionConnectorFilter().ifPresent(list -> list.setDataNeedId(newDataNeed.id()));

        if (newDataNeed instanceof TimeframedDataNeed timeframedDataNeed)
            timeframedDataNeed.duration().setDataNeedId(newDataNeed.id());

        LOGGER.info("Saving new data need with ID '{}'", newDataNeed.id());

        return initializeAndUnproxy(repository.save(newDataNeed));
    }

    /**
     * Returns a list of all data needs saved in the database.
     */
    public List<DataNeed> findAll() {
        return repository.findAll().stream()
                         .map(DataNeedsDbService::initializeAndUnproxy)
                         .toList();
    }

    public boolean existsById(String id) {
        return repository.existsById(id);
    }

    public void deleteById(String id) {
        LOGGER.atDebug()
              .addArgument(() -> UUID.fromString(id))
              .log("Deleting data need with ID {}");
        repository.deleteById(id);
    }
}
