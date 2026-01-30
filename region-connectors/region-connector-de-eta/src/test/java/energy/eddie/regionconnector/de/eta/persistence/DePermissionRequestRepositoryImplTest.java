package energy.eddie.regionconnector.de.eta.persistence;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DePermissionRequestRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ResultSet resultSet;

    private DePermissionRequestRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new DePermissionRequestRepositoryImpl(jdbcTemplate);
    }

    @Test
    void findByPermissionIdWhenFoundShouldReturnOptionalWithRequest() {
        DePermissionRequest request = mock(DePermissionRequest.class);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(List.of(request));

        var result = repository.findByPermissionId("id-1");

        assertThat(result).hasValue(request);
    }

    @Test
    void findByPermissionIdWhenNotFoundShouldReturnEmptyOptional() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(Collections.emptyList());

        var result = repository.findByPermissionId("id-1");

        assertThat(result).isEmpty();
    }

    @Test
    void getByPermissionIdWhenFoundShouldReturnRequest() {
        DePermissionRequest request = mock(DePermissionRequest.class);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(List.of(request));

        var result = repository.getByPermissionId("id-1");

        assertThat(result).isEqualTo(request);
    }

    @Test
    void saveShouldThrowUnsupportedOperationException() {
        assertThatThrownBy(() -> repository.save(mock(DePermissionRequest.class)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void findByStatusShouldReturnListOfRequests() {
        DePermissionRequest request = mock(DePermissionRequest.class);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(List.of(request));

        var result = repository.findByStatus(PermissionProcessStatus.CREATED);

        assertThat(result).containsExactly(request);
    }

    @Test
    void findStalePermissionRequestsShouldReturnListOfRequests() {
        DePermissionRequest request = mock(DePermissionRequest.class);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt()))
                .thenReturn(List.of(request));

        var result = repository.findStalePermissionRequests(30);

        assertThat(result).containsExactly(request);
    }

    @Test
    void rowMapperShouldMapAllFieldsCorrectly() throws SQLException {
        when(resultSet.getString("permission_id")).thenReturn("p1");
        when(resultSet.getString("data_source_connection_id")).thenReturn("c1");
        when(resultSet.getString("metering_point_id")).thenReturn("m1");
        when(resultSet.getString("data_need_id")).thenReturn("dn1");
        when(resultSet.getString("message")).thenReturn("msg");
        when(resultSet.getString("cause")).thenReturn("cause");
        when(resultSet.getString("granularity")).thenReturn("PT15M");
        when(resultSet.getString("energy_type")).thenReturn("ELECTRICITY");
        when(resultSet.getString("status")).thenReturn("CREATED");

        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        Timestamp timestamp = Timestamp.valueOf(now);
        when(resultSet.getTimestamp("data_start")).thenReturn(timestamp);
        when(resultSet.getTimestamp("data_end")).thenReturn(timestamp);
        when(resultSet.getTimestamp("created")).thenReturn(timestamp);

        DePermissionRequestRepositoryImpl.PermissionRequestRowMapper mapper = 
                new DePermissionRequestRepositoryImpl.PermissionRequestRowMapper();
        
        DePermissionRequest result = mapper.mapRow(resultSet, 1);

        assertThat(result.permissionId()).isEqualTo("p1");
        assertThat(result.connectionId()).isEqualTo("c1");
        assertThat(result.meteringPointId()).isEqualTo("m1");
        assertThat(result.dataNeedId()).isEqualTo("dn1");
        assertThat(result.message()).hasValue("msg");
        assertThat(result.cause()).hasValue("cause");
        assertThat(result.granularity()).isEqualTo(Granularity.PT15M);
        assertThat(result.energyType()).isEqualTo(EnergyType.ELECTRICITY);
        assertThat(result.status()).isEqualTo(PermissionProcessStatus.CREATED);
        assertThat(result.start()).isEqualTo(now.toLocalDate());
        assertThat(result.end()).isEqualTo(now.toLocalDate());
        assertThat(result.created()).isNotNull();
    }

    @Test
    void rowMapperShouldHandleNullFields() throws SQLException {
        when(resultSet.getString(anyString())).thenReturn(null);
        when(resultSet.getTimestamp(anyString())).thenReturn(null);

        DePermissionRequestRepositoryImpl.PermissionRequestRowMapper mapper = 
                new DePermissionRequestRepositoryImpl.PermissionRequestRowMapper();
        
        DePermissionRequest result = mapper.mapRow(resultSet, 1);

        assertThat(result.granularity()).isNull();
        assertThat(result.energyType()).isNull();
        assertThat(result.status()).isNull();
        assertThat(result.start()).isNull();
        assertThat(result.end()).isNull();
        assertThat(result.created()).isNull();
        assertThat(result.message()).isEmpty();
    }
}
