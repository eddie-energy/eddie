package energy.eddie.core.dataneeds;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dataneeds-from-db")
class DataNeedsDbRepositoryTest {

    @Autowired
    private DataNeedsDbRepository repo;

    @BeforeEach
    void setUp() {
        repo.deleteAll();
    }

    @Test
    void testSaveAndDelete() {
        assertThat(repo.count()).isZero();
        repo.save(DataNeedImplTest.EXAMPLE_DATA_NEED);
        assertThat(repo.count()).isEqualTo(1);
        assertThat(repo.findById(DataNeedImplTest.EXAMPLE_DATA_NEED_KEY)).hasValue(DataNeedImplTest.EXAMPLE_DATA_NEED);
        repo.delete(DataNeedImplTest.EXAMPLE_DATA_NEED);
        assertThat(repo.count()).isZero();
    }

    @Test
    void findAllIds() {
        assertThat(repo.count()).isZero();
        repo.save(DataNeedImplTest.EXAMPLE_DATA_NEED);
        assertThat(repo.findAllIds()).containsOnly(DataNeedImplTest.EXAMPLE_DATA_NEED_KEY);
        final var otherId = "OTHER_ID";
        var other = DataNeedImplTest.copy(DataNeedImplTest.EXAMPLE_DATA_NEED);
        other.setId(otherId);
        repo.save(other);
        assertThat(repo.findAllIds()).containsOnly(DataNeedImplTest.EXAMPLE_DATA_NEED_KEY, otherId);
    }
}