// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.persistence.specifications;

import energy.eddie.cim.v1_12.vhd.*;
import energy.eddie.outbound.rest.RestTestConfig;
import energy.eddie.outbound.rest.model.cim.v1_12.ValidatedHistoricalDataMarketDocumentModel;
import energy.eddie.outbound.rest.persistence.PersistenceConfig;
import energy.eddie.outbound.rest.persistence.cim.v1_12.ValidatedHistoricalDataMarketDocumentRepository;
import energy.eddie.outbound.rest.tasks.InsertionTask;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import reactor.core.publisher.Flux;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PersistenceConfig.class, RestTestConfig.class})
@Testcontainers
class ValidatedHistoricalDataV112PersistenceTest {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:15-alpine");
    @Autowired
    private ValidatedHistoricalDataMarketDocumentRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void insertsFiltersEachNestedMetadataFieldAndDeletesByRetentionTime() {
        var payload = new Envelope().withMessageDocumentHeader(new MessageDocumentHeader().withMetaInformation(
                new MetaInformation().withRequestPermissionId("pid").withConnectionId("cid").withDataNeedId("dnid")
                        .withRegionConnector("at-eda").withRegionCountry("AT")));
        try (var task = new InsertionTask<>(Flux.just(payload), repository, ValidatedHistoricalDataMarketDocumentModel::new)) {
            repository.flush();
            assertThat(repository.findAll()).hasSize(1);
            for (int index = 0; index < 7; index++) {
                var matching = List.of("pid", "cid", "dnid", "at", "at-eda");
                var params = new ArrayList<Optional<String>>(Collections.nCopies(5, Optional.empty()));
                Optional<ZonedDateTime> from = Optional.empty();
                Optional<ZonedDateTime> to = Optional.empty();
                if (index < 5) {
                    params.set(index, Optional.of(matching.get(index)));
                }
                if (index == 5) {
                    from = Optional.of(ZonedDateTime.now(ZoneOffset.UTC).minusHours(1));
                }
                if (index == 6) {
                    to = Optional.of(ZonedDateTime.now(ZoneOffset.UTC).plusHours(1));
                }
                var query = CimSpecification.<ValidatedHistoricalDataMarketDocumentModel>buildQueryForV1_12(
                        params.get(0), params.get(1), params.get(2), params.get(3), params.get(4), from, to);
                assertThat(repository.findAll(query)).hasSize(1);
                if (index < 5) {
                    params.set(index, Optional.of("missing"));
                }
                if (index == 5) {
                    from = Optional.of(ZonedDateTime.now(ZoneOffset.UTC).plusHours(1));
                }
                if (index == 6) {
                    to = Optional.of(ZonedDateTime.now(ZoneOffset.UTC).minusHours(1));
                }
                var mismatch = CimSpecification.<ValidatedHistoricalDataMarketDocumentModel>buildQueryForV1_12(
                        params.get(0), params.get(1), params.get(2), params.get(3), params.get(4), from, to);
                assertThat(repository.findAll(mismatch)).isEmpty();
            }
            entityManager.clear();
            var beforeRetention = ZonedDateTime.now(ZoneOffset.UTC).minusHours(1);
            repository.delete(InsertionTimeSpecification.insertedBeforeEquals(beforeRetention));
            assertThat(repository.count()).isEqualTo(1);
            entityManager.clear();
            var afterRetention = ZonedDateTime.now(ZoneOffset.UTC).plusHours(1);
            repository.delete(InsertionTimeSpecification.insertedBeforeEquals(afterRetention));
            assertThat(repository.count()).isZero();
        }
    }
}
