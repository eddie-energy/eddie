package energy.eddie.framework.dataneeds;

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

    @Test
    void testSaveAndDelete() {
        assertThat(repo.count()).isZero();
        repo.save(DataNeedTest.EXAMPLE_DATA_NEED);
        assertThat(repo.count()).isEqualTo(1);
        assertThat(repo.findById(DataNeedTest.EXAMPLE_DATA_NEED_KEY)).hasValue(DataNeedTest.EXAMPLE_DATA_NEED);
        repo.delete(DataNeedTest.EXAMPLE_DATA_NEED);
        assertThat(repo.count()).isZero();
    }
}