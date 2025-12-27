package energy.eddie.regionconnector.de.eta.persistence;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

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

        assertThat(result).isPresent().contains(request);
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

        assertThat(result).hasSize(1).contains(request);
    }

    @Test
    void findStalePermissionRequestsShouldReturnListOfRequests() {
        DePermissionRequest request = mock(DePermissionRequest.class);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt()))
                .thenReturn(List.of(request));

        var result = repository.findStalePermissionRequests(30);

        assertThat(result).hasSize(1).contains(request);
    }
}
