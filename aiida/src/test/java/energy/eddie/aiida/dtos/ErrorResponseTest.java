package energy.eddie.aiida.dtos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorResponseTest {
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void givenEmptyErrors_resultsInEmptyJsonArray() throws JsonProcessingException {
        ErrorResponse errorResponse = new ErrorResponse(List.of());
        String json = mapper.writeValueAsString(errorResponse);

        assertEquals("{\"errors\":[]}", json);
    }
}