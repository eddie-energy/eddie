package energy.eddie.regionconnector.de.eta.persistence;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequest;
import energy.eddie.regionconnector.de.eta.permission.request.DePermissionRequestRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class DePermissionRequestRepositoryImpl implements DePermissionRequestRepository {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionRequestRowMapper rowMapper;

    private static final String BASE_QUERY = """
        SELECT permission_id, data_source_connection_id, metering_point_id,
               permission_start, permission_end, data_start, data_end,
               granularity, energy_type, status,
               data_need_id,
               data_need_id_str,
               created,
               message, cause,
               latest_reading
        FROM de_eta.eta_permission_request
        """;

    public DePermissionRequestRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = new PermissionRequestRowMapper();
    }

    @Override
    public Optional<DePermissionRequest> findByPermissionId(String permissionId) {
        String query = BASE_QUERY + " WHERE permission_id = ?";
        List<DePermissionRequest> results = jdbcTemplate.query(query, rowMapper, permissionId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    @SuppressWarnings("NullAway")
    public DePermissionRequest getByPermissionId(String permissionId) {
        return findByPermissionId(permissionId).orElse(null);
    }

    @Override
    public void save(DePermissionRequest request) {
        // Updates the physical TABLE to avoid "cannot update view" errors
        String updateQuery = """
            UPDATE de_eta.permission_event
            SET latest_reading = ?
            WHERE permission_id = ?
            """;

        Timestamp latestReadingTs = request.latestReading()
                .map(zdt -> Timestamp.from(zdt.toInstant()))
                .orElse(null);

        jdbcTemplate.update(updateQuery, latestReadingTs, request.permissionId());
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
             AND created < ?
            """;
        Timestamp threshold = Timestamp.valueOf(java.time.LocalDateTime.now(ZoneId.systemDefault()).minusDays(days));
        return jdbcTemplate.query(query, rowMapper, threshold);
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM de_eta.permission_event");
    }

    private static class PermissionRequestRowMapper implements RowMapper<DePermissionRequest> {

        @Override
        @SuppressWarnings("NullAway")
        public DePermissionRequest mapRow(ResultSet rs, int rowNum) throws SQLException {

            ZonedDateTime createdDate = getZonedDate(rs, "created", ZoneId.systemDefault());
            if (createdDate == null) {
                createdDate = ZonedDateTime.now(ZoneId.systemDefault());
            }

            LocalDate startDate = getLocalDate(rs, "data_start");
            if (startDate == null) startDate = LocalDate.EPOCH;

            LocalDate endDate = getLocalDate(rs, "data_end");
            if (endDate == null) endDate = LocalDate.EPOCH;

            return DePermissionRequest.builder()
                    .permissionId(rs.getString("permission_id"))
                    .connectionId(rs.getString("data_source_connection_id"))
                    .meteringPointId(rs.getString("metering_point_id"))

                    .start(startDate)
                    .end(endDate)

                    .granularity(parseEnum(rs.getString("granularity"), energy.eddie.api.agnostic.Granularity.class))
                    .energyType(parseEnum(rs.getString("energy_type"), energy.eddie.api.agnostic.data.needs.EnergyType.class))
                    .status(parseEnum(rs.getString("status"), PermissionProcessStatus.class))

                    .dataNeedId(rs.getString("data_need_id_str"))

                    .created(createdDate)
                    .latestReading(getZonedDate(rs, "latest_reading", ZoneId.of("UTC")))

                    .message(rs.getString("message"))
                    .cause(rs.getString("cause"))
                    .build();
        }

        @Nullable
        private <E extends Enum<E>> E parseEnum(String value, Class<E> enumClass) {
            if (value == null) return null;
            return Enum.valueOf(enumClass, value);
        }

        @Nullable
        private LocalDate getLocalDate(ResultSet rs, String columnLabel) throws SQLException {
            java.sql.Date date = rs.getDate(columnLabel);
            return (date != null) ? date.toLocalDate() : null;
        }

        @Nullable
        private ZonedDateTime getZonedDate(ResultSet rs, String columnLabel, ZoneId zone) throws SQLException {
            Timestamp ts = rs.getTimestamp(columnLabel);
            return (ts != null) ? ts.toInstant().atZone(zone) : null;
        }
    }
}