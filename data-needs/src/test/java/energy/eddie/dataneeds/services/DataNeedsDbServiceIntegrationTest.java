package energy.eddie.dataneeds.services;

import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.persistence.DataNeedsRepository;
import org.hibernate.proxy.HibernateProxy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration/data-needs",
        "spring.flyway.schemas=data_needs"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class DataNeedsDbServiceIntegrationTest {
    @SuppressWarnings("unused")
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer("postgres:15-alpine");
    @Autowired
    private DataNeedsRepository repository;

    public static DataNeed createDataNeed() {
        return new AccountingPointDataNeed("ac", "desc", "purpose", "https://localhost", true, null);
    }

    @Test
    void testGetDataNeedIdsAndNames_returnsUnproxiedImplementation() {
        // Given
        var service = new DataNeedsDbService(repository);
        repository.save(createDataNeed());

        // When
        var res = service.getDataNeedIdsAndNames();

        // Then
        assertThat(res).singleElement().isNotInstanceOf(HibernateProxy.class);
    }

    @Test
    void testFindById_returnsUnproxiedImplementation() {
        // Given
        var service = new DataNeedsDbService(repository);
        var dn = repository.save(createDataNeed());

        // When
        var res = service.findById(dn.id());

        // Then
        assertThat(res).isNotEmpty().isNotInstanceOf(HibernateProxy.class);
    }

    @Test
    void testGetById_returnsUnproxiedImplementation() {
        // Given
        var service = new DataNeedsDbService(repository);
        var dn = repository.save(createDataNeed());

        // When
        var res = service.getById(dn.id());

        // Then
        assertThat(res).isNotInstanceOf(HibernateProxy.class);
    }

    @Test
    void testSaveNewDataNeed_returnsUnproxiedImplementation() {
        // Given
        var service = new DataNeedsDbService(repository);

        // When
        var res = service.saveNewDataNeed(createDataNeed());

        // Then
        assertThat(res).isNotInstanceOf(HibernateProxy.class);
    }

    @Test
    void testFindAll_returnsUnproxiedImplementation() {
        // Given
        var service = new DataNeedsDbService(repository);
        repository.save(createDataNeed());

        // When
        var res = service.findAll();

        // Then
        assertThat(res).singleElement().isNotInstanceOf(HibernateProxy.class);
    }
}
