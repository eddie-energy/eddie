package energy.eddie.framework.dataneeds;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static energy.eddie.framework.dataneeds.DataNeedTest.EXAMPLE_DATA_NEED;
import static energy.eddie.framework.dataneeds.DataNeedTest.EXAMPLE_DATA_NEED_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest()
@ActiveProfiles("dataneeds-from-db")
class DataNeedsDbServiceTest {

    private static final String NONEXISTENT_DATA_NEED_ID = "NONEXISTENT_DATA_NEED";

    @Autowired
    private DataNeedsService dataNeedsDbService;

    @MockBean
    private DataNeedsDbRepository dataNeedsDbRepository;

    @Test
    void testGetDataNeed() {
        given(dataNeedsDbRepository.findById(EXAMPLE_DATA_NEED_KEY)).willReturn(Optional.of(EXAMPLE_DATA_NEED));
        given(dataNeedsDbRepository.findById(NONEXISTENT_DATA_NEED_ID)).willReturn(Optional.empty());
        assertThat(dataNeedsDbService.getDataNeed(EXAMPLE_DATA_NEED_KEY)).isPresent().get()
                .extracting("type").isEqualTo(DataType.HISTORICAL_VALIDATED_CONSUMPTION_DATA);
        assertThat(dataNeedsDbService.getDataNeed(NONEXISTENT_DATA_NEED_ID)).isEmpty();
    }

}
