package energy.eddie.dataneeds.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.dataneeds.duration.RelativeDuration;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.persistence.DataNeedsNameAndIdProjection;
import energy.eddie.dataneeds.persistence.DataNeedsRepository;
import energy.eddie.dataneeds.utils.DataNeedUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static energy.eddie.dataneeds.web.DataNeedsControllerTest.EXAMPLE_ACCOUNTING_POINT_DATA_NEED;
import static energy.eddie.dataneeds.web.DataNeedsControllerTest.EXAMPLE_VHD_DATA_NEED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataNeedsDbServiceTest {
    @Mock
    private DataNeedsRepository mockRepository;
    @InjectMocks
    private DataNeedsDbService service;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private DataNeed exampleVhd;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        exampleVhd = mapper.readValue(EXAMPLE_VHD_DATA_NEED, DataNeed.class);
    }

    @Test
    void givenDataNeeds_getDataNeedIdsAndNames_returnsNameAndIds() {
        // Given
        var first = new DataNeedsNameAndIdProjection() {
            @Override
            public String getId() {
                return "123";
            }

            @Override
            public String getName() {
                return "Name";
            }
        };
        var second = new DataNeedsNameAndIdProjection() {
            @Override
            public String getId() {
                return "fooBar";
            }

            @Override
            public String getName() {
                return "Accounting Point Need";
            }
        };
        when(mockRepository.findAllBy()).thenReturn(List.of(first, second));

        // When
        List<DataNeedsNameAndIdProjection> list = service.getDataNeedIdsAndNames();

        // Then
        assertThat(list)
                .hasSize(2)
                .extracting(DataNeedsNameAndIdProjection::getId)
                .containsExactlyInAnyOrder("123", "fooBar");
    }

    @Test
    void givenNonExistingId_findById_returnsEmptyOptional() {
        // When
        Optional<DataNeed> nonExisting = service.findById("nonExisting");

        // Then
        assertTrue(nonExisting.isEmpty());
    }

    @Test
    void givenExistingId_findById_returnsDataNeed() {
        // Given
        String id = "123";
        when(mockRepository.findById(id)).thenReturn(Optional.of(exampleVhd));

        // When
        Optional<DataNeed> optional = service.findById(id);

        // Then
        assertTrue(optional.isPresent());
        assertEquals("123", optional.get().id());
        assertInstanceOf(ValidatedHistoricalDataDataNeed.class, optional.get());
        ValidatedHistoricalDataDataNeed vhd = (ValidatedHistoricalDataDataNeed) optional.get();
        assertInstanceOf(RelativeDuration.class, vhd.duration());
        assertTrue(((RelativeDuration) vhd.duration()).stickyStartCalendarUnit().isEmpty());
    }

    @Test
    void givenDataNeed_saveNewDataNeed_callsRepository_andSetsId() {
        // Given
        when(mockRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        // When
        var saved = service.saveNewDataNeed(exampleVhd);

        // Then
        verify(mockRepository).save(argThat(arg -> arg.id().equals(saved.id())));
        // service should set an ID and ignore any other value
        assertNotEquals("123", saved.id());
        // the id of duration should be set to the one of the data need for correct association
        assertEquals(saved.id(), ((ValidatedHistoricalDataDataNeed) saved).duration().dataNeedId());
        verifyNoMoreInteractions(mockRepository);
    }

    @Test
    void givenDataNeeds_findAll_returnsListOfAll() throws JsonProcessingException {
        // Given
        DataNeed accountExample = mapper.readValue(EXAMPLE_ACCOUNTING_POINT_DATA_NEED, DataNeed.class);
        when(mockRepository.findAll()).thenReturn(List.of(accountExample, exampleVhd));

        // When
        var list = service.findAll();

        // Then
        assertThat(list)
                .hasSize(2)
                .extracting(DataNeed::id)
                .containsExactlyInAnyOrder("123", "fooBar");
    }

    @Test
    void verify_existsById_callsRepository() {
        // When
        String id = "foo";
        service.existsById(id);

        // Then
        verify(mockRepository).existsById(id);
        verifyNoMoreInteractions(mockRepository);
    }

    @Test
    void verify_deleteById_callsRepository() {
        // When
        String id = "foo";
        service.deleteById(id);

        // Then
        verify(mockRepository).deleteById(id);
        verifyNoMoreInteractions(mockRepository);
    }


    @Test
    void givenInvalidDataNeedId_findAndCalculateRelativeStartAndEnd_throwsException() {
        // When, Then
        assertThrows(DataNeedNotFoundException.class, () ->
                service.findDataNeedAndCalculateStartAndEnd("UNKNOWN ID",
                                                            LocalDate.now(ZoneId.of("UTC")),
                                                            Period.parse("P0D"),
                                                            Period.parse("P0D")));
    }

    @Test
    void givenValidDataNeedId_findAndCalculateRelativeStartAndEnd_callsUtils() {
        // Given
        final String id = "LAST_3_MONTHS_ONE_MEASUREMENT_PER_DAY";
        when(mockRepository.findById(id)).thenReturn(Optional.of(exampleVhd));


        try (MockedStatic<DataNeedUtils> utilsStatic = Mockito.mockStatic(DataNeedUtils.class)) {
            // When
            assertDoesNotThrow(() -> service.findDataNeedAndCalculateStartAndEnd(id,
                                                                                 LocalDate.now(ZoneId.of("UTC")),
                                                                                 Period.parse("P0D"),
                                                                                 Period.parse("P0D")));

            // Then
            utilsStatic.verify(() -> DataNeedUtils.calculateRelativeStartAndEnd(any(), any(), any(), any()));
        }
    }
}
