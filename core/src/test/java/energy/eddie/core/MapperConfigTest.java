package energy.eddie.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import energy.eddie.api.agnostic.ConnectionStatusMessage;
import energy.eddie.api.agnostic.DataSourceInformation;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.core.dtos.SimpleDataSourceInformation;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapperConfigTest {

    @Test
    void canDeserializeDataSourceInformation() throws JsonProcessingException {
        // Given
        DataSourceInformation dataSourceInformation = new SimpleDataSourceInformation(
                "at",
                "at-eda",
                "eda",
                "eda"
        );
        var msg = new ConnectionStatusMessage(
                "cid",
                "pid",
                "dnid",
                dataSourceInformation,
                ZonedDateTime.now(ZoneOffset.UTC),
                PermissionProcessStatus.CREATED,
                "",
                null
        );
        var builder = new Jackson2ObjectMapperBuilder();
        new MapperConfig().jsonCustomizer().customize(builder);
        var mapper = builder
                .modules(new JavaTimeModule())
                .build();
        var str = mapper.writeValueAsString(msg);

        // When
        var res = mapper.readValue(str, ConnectionStatusMessage.class);

        // Then
        assertEquals(msg.dataSourceInformation(), res.dataSourceInformation());
    }
}