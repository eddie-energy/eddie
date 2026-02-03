package energy.eddie.regionconnector.de.eta.persistence;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.DeDataSourceInformation;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-based implementation of DePermissionRequestRepository.
 * This repository reads from the aggregated view created by the event sourcing table.
 */
@Repository
public class DePermissionRequestRepositoryImpl implements DePermissionRequestRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final PermissionRequestRowMapper rowMapper;
    
    private static final String BASE_QUERY = """
        SELECT permission_id, data_source_connection_id, metering_point_id,
               permission_start, permission_end, data_start, data_end,
               granularity, energy_type, status, data_need_id, created,
               message, cause
        FROM de_eta.eta_permission_request
        """;

    public DePermissionRequestRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = new PermissionRequestRowMapper();
    }

    @Override
    public Optional<DePermissionRequest> findByPermissionId(String permissionId) {
        String query = BASE_QUERY + " WHERE permission_id::text = ?";
        List<DePermissionRequest> results = jdbcTemplate.query(query, rowMapper, permissionId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    @SuppressWarnings("NullAway") // Can return null if not found
    public DePermissionRequest getByPermissionId(String permissionId) {
        return findByPermissionId(permissionId).orElse(null);
    }

    @Override
    public void save(DePermissionRequest request) {
        // This is a read-only repository that reads from the event-sourced view
        // Saving is done through the event store, not directly
        throw new UnsupportedOperationException("Cannot save directly to view - use event store");
    }

    @Override
    public List<DePermissionRequest> findByStatus(PermissionProcessStatus status) {
        String query = BASE_QUERY + " WHERE status = ?";
        return jdbcTemplate.query(query, rowMapper, status.name());
    }

    @Override
    public List<DePermissionRequest> findStalePermissionRequests(int days) {
        String query = BASE_QUERY + """
             WHERE status IN ('REQUESTED', 'PENDING_CONSENT')
             AND created < NOW() - INTERVAL '? days'
            """;
        return jdbcTemplate.query(query, rowMapper, days);
    }

    /**
     * RowMapper to convert database rows to DePermissionRequest objects
     */
    static class PermissionRequestRowMapper implements RowMapper<DePermissionRequest> {
        @Override
        public DePermissionRequest mapRow(ResultSet rs, int rowNum) throws SQLException {
            var builder = DePermissionRequest.builder()
                    .permissionId(rs.getString("permission_id"))
                    .connectionId(rs.getString("data_source_connection_id"))
                    .meteringPointId(rs.getString("metering_point_id"))
                    .dataNeedId(rs.getString("data_need_id"))
                    .dataSourceInformation(new DeDataSourceInformation())
                    .message(rs.getString("message"))
                    .cause(rs.getString("cause"));

            // Handle nullable enum fields
            var granularity = rs.getString("granularity");
            if (granularity != null) {
                builder.granularity(energy.eddie.api.agnostic.Granularity.valueOf(granularity));
            }

            var energyType = rs.getString("energy_type");
            if (energyType != null) {
                builder.energyType(energy.eddie.api.agnostic.data.needs.EnergyType.valueOf(energyType));
            }

            var status = rs.getString("status");
            if (status != null) {
                builder.status(PermissionProcessStatus.valueOf(status));
            }

            // Handle nullable timestamp fields
            var dataStart = rs.getTimestamp("data_start");
            if (dataStart != null) {
                builder.start(dataStart.toLocalDateTime().toLocalDate());
            }

            var dataEnd = rs.getTimestamp("data_end");
            if (dataEnd != null) {
                builder.end(dataEnd.toLocalDateTime().toLocalDate());
            }

            var created = rs.getTimestamp("created");
            if (created != null) {
                builder.created(created.toInstant().atZone(ZoneId.systemDefault()));
            }

            return builder.build();
        }
    }
}
