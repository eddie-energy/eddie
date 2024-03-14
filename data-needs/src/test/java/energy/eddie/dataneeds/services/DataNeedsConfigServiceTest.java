package energy.eddie.dataneeds.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.dataneeds.exceptions.DataNeedAlreadyExistsException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.persistence.DataNeedsNameAndIdProjection;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class DataNeedsConfigServiceTest {
    @MockBean
    private DataNeedsService mockService;
    @Autowired
    private ApplicationContext context;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void givenValidADataNeeds_addsAll() throws IOException {
        // Given
        String file = new ClassPathResource("test-valid-data-needs.json").getFile().getAbsolutePath();

        // When
        DataNeedsConfigService service = assertDoesNotThrow(() -> new DataNeedsConfigService(file, mapper, context));

        // Then
        assertThat(service.getDataNeedIdsAndNames())
                .extracting(DataNeedsNameAndIdProjection::getId)
                .containsExactlyInAnyOrder("FUTURE_NEAR_REALTIME_DATA",
                                           "NEXT_10_DAYS_ONE_MEASUREMENT_PER_DAY",
                                           "LAST_3_MONTHS_ONE_MEASUREMENT_PER_DAY");
        Optional<DataNeed> example = service.findById("NEXT_10_DAYS_ONE_MEASUREMENT_PER_DAY");
        assertTrue(example.isPresent());
        // created timestamp is ignored when reading from JSON
        assertNull(example.get().createdAt());
        assertInstanceOf(ValidatedHistoricalDataDataNeed.class, example.get());
        assertEquals("MyTestName", example.get().name());
    }

    @Test
    void givenInvalidDataNeed_throwsException() throws IOException {
        String file = new ClassPathResource("test-invalid-data-needs.json").getFile().getAbsolutePath();

        // When, Then
        var thrown = assertThrows(ValidationException.class, () -> new DataNeedsConfigService(file, mapper, context));
        assertThat(thrown.getMessage()).startsWith("Failed to validate data need with ID 'INVALID'");
    }

    @Test
    void givenDuplicateDataNeedId_throwsException() throws IOException {
        String file = new ClassPathResource("test-duplicate-data-needs.json").getFile().getAbsolutePath();

        // When, Then
        var thrown = assertThrows(DataNeedAlreadyExistsException.class,
                                  () -> new DataNeedsConfigService(file, mapper, context));
        assertEquals("Data need with ID 'LAST_3_MONTHS_ONE_MEASUREMENT_PER_DAY' already exists.", thrown.getMessage());
    }
}
