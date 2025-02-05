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
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class DataNeedsConfigServiceTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    @MockitoBean
    private DataNeedsConfigService unusedMockService;
    @Autowired
    private ApplicationContext context;

    @Test
    void givenValidADataNeeds_addsAll() throws IOException {
        // Given
        String file = new ClassPathResource("test-valid-data-needs.json").getFile().getAbsolutePath();

        // When
        DataNeedsConfigService service = assertDoesNotThrow(() -> new DataNeedsConfigService(file, mapper, context));

        // Then
        assertThat(service.getDataNeedIdsAndNames())
                .extracting(DataNeedsNameAndIdProjection::getId)
                .containsExactlyInAnyOrder("5dc71d7e-e8cd-4403-a3a8-d3c095c97a84",
                                           "dcbc1c74-37bd-4c5b-ab2e-fd0be9c1edf3",
                                           "9bd0668f-cc19-40a8-99db-dc2cb2802b17");
        Optional<DataNeed> example = service.findById("dcbc1c74-37bd-4c5b-ab2e-fd0be9c1edf3");
        assertTrue(example.isPresent());
        // created timestamp is ignored when reading from JSON
        assertNull(example.get().createdAt());
        assertInstanceOf(ValidatedHistoricalDataDataNeed.class, example.get());
        assertEquals("MyTestName", example.get().name());
    }

    @Test
    void givenInvalidDataNeedName_throwsException() throws IOException {
        String file = new ClassPathResource("test-invalid-data-needs-name.json").getFile().getAbsolutePath();

        // When, Then
        var thrown = assertThrows(ValidationException.class, () -> new DataNeedsConfigService(file, mapper, context));
        assertThat(thrown.getMessage()).startsWith(
                "Failed to validate data need with ID 'b02e137a-cb39-4ca2-adce-c7193d16322d'");
    }

    @Test
    void givenInvalidDataNeedId_throwsException() throws IOException {
        String file = new ClassPathResource("test-invalid-data-needs-id.json").getFile().getAbsolutePath();

        // When, Then
        var thrown = assertThrows(ValidationException.class, () -> new DataNeedsConfigService(file, mapper, context));
        assertThat(thrown.getMessage()).startsWith("Data need ID 'This is not a UUID' is not a valid UUID");
    }

    @Test
    void givenDuplicateDataNeedId_throwsException() throws IOException {
        String file = new ClassPathResource("test-duplicate-data-needs.json").getFile().getAbsolutePath();

        // When, Then
        var thrown = assertThrows(DataNeedAlreadyExistsException.class,
                                  () -> new DataNeedsConfigService(file, mapper, context));
        assertEquals("Data need with ID '9bd0668f-cc19-40a8-99db-dc2cb2802b17' already exists.", thrown.getMessage());
    }
}
